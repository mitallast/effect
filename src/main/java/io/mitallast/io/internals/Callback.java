package io.mitallast.io.internals;

import io.mitallast.either.Either;
import io.mitallast.kernel.Unit;
import org.apache.logging.log4j.LogManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static io.mitallast.io.internals.TrampolineEC.immediate;

public interface Callback {
    static <A> Consumer<Either<Throwable, A>> report() {
        var logger = LogManager.getLogger();
        return e -> e.fold(
            err -> {
                logger.error(err);
                return Unit.unit();
            },
            a -> {
                logger.trace(a);
                return Unit.unit();
            }
        );
    }

    static <A> Consumer<A> dummy() {
        return a -> {
        };
    }

    static <A> Consumer<Either<Throwable, A>> async(Consumer<Either<Throwable, A>> cb) {
        return async(null, cb);
    }

    static <A> Consumer<Either<Throwable, A>> async(IOConnection conn, Consumer<Either<Throwable, A>> cb) {
        return value -> immediate.execute(() -> {
            if (conn != null) conn.pop();
            cb.accept(value);
        });
    }

    static <A> Consumer<Either<Throwable, A>> asyncIdempotent(IOConnection conn, Consumer<Either<Throwable, A>> cb) {
        return new AsyncIdempotentCallback<>(conn, cb);
    }

    static <A> Consumer<Either<Throwable, A>> promise(CompletableFuture<A> p) {
        return e -> e.fold(p::completeExceptionally, p::complete);
    }
}

class AsyncIdempotentCallback<A> implements Consumer<Either<Throwable, A>>, Runnable {
    private final IOConnection conn;
    private final Consumer<Either<Throwable, A>> cb;
    private final AtomicBoolean canCall = new AtomicBoolean(true);
    private volatile Either<Throwable, A> value = null;

    private AsyncIdempotentCallback(IOConnection conn, Consumer<Either<Throwable, A>> cb) {
        this.conn = conn;
        this.cb = cb;
    }

    @Override
    public void run() {
        cb.accept(value);
    }

    @Override
    public void accept(Either<Throwable, A> throwableAEither) {
        if (canCall.getAndSet(false)) {
            if (conn != null) conn.pop();
            this.value = value;
            immediate.execute(this);
        } else {
            if (value.isLeft()) {
                immediate.reportFailure(value.left().get());
            }
        }
    }
}