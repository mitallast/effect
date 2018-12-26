package io.mitallast.io.internals;

import io.mitallast.either.Either;
import io.mitallast.either.Try;
import io.mitallast.io.IO;
import io.mitallast.list.List;
import io.mitallast.maybe.Maybe;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

import static io.mitallast.concurrent.ExecutionContext.blocking;

public interface IOPlatform {
    static <A> Maybe<A> unsafeResync(IO<A> ioa, Duration limit) {
        final var latch = new OneShotLatch();
        final AtomicReference<Either<Throwable, A>> ref = new AtomicReference<>();

        ioa.unsafeRunAsync(a -> {
            // Reading from `ref` happens after the block on `latch` is
            // over, there's a happens-before relationship, so no extra
            // synchronization is needed for visibility
            ref.set(a);
            latch.releaseShared(1);
        });

        if (!limit.isZero()) {
            blocking(() -> {
                latch.acquireSharedInterruptibly(1);
                return null;
            });
        }

        var result = ref.get();
        if (result == null) {
            return Maybe.none();
        } else if (result.isRight()) {
            return Maybe.apply(result.right().get());
        } else {
            return throwsUnchecked(result.left().get());
        }
    }

    @SuppressWarnings("unchecked")
    static <R, T extends Throwable> R throwsUnchecked(Throwable toThrow) throws T {
        // Since the type is erased, this cast actually does nothing!!!
        // we can throw any exception
        throw (T) toThrow;
    }

    int fusionMaxStackDepth =
        Maybe.apply(System.getProperty("cats.effect.fusionMaxStackDepth", ""))
            .filter(s -> s != null && !s.isEmpty())
            .flatMap(s -> Try.apply(() -> Integer.parseInt(s)).toOption())
            .filter(i -> i > 0)
            .map(i -> i - 1)
            .getOrElse(127);

    /**
     * Composes multiple errors together, meant for those cases in which
     * error suppression, due to a second error being triggered, is not
     * acceptable.
     * <p>
     * On top of the JVM this function uses `Throwable#addSuppressed`,
     * available since Java 7. On top of JavaScript the function would return
     * a `CompositeException`.
     */
    static Throwable composeErrors(Throwable first, Throwable... rest) {
        for (Throwable e : rest) {
            first.addSuppressed(e);
        }
        return first;
    }

    static Throwable composeErrors(Throwable first, List<Throwable> rest) {
        while (rest.nonEmpty()) {
            first.addSuppressed(rest.head());
            rest = rest.tail();
        }
        return first;
    }
}

final class OneShotLatch extends AbstractQueuedSynchronizer {
    @Override
    protected int tryAcquireShared(int arg) {
        if (getState() != 0) return 1;
        else return -1;
    }

    @Override
    protected boolean tryReleaseShared(int arg) {
        setState(1);
        return true;
    }
}
