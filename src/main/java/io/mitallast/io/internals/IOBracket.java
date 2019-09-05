package io.mitallast.io.internals;

import io.mitallast.either.Either;
import io.mitallast.io.ExitCase;
import io.mitallast.io.IO;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Function2;
import io.mitallast.lambda.Function4;

import java.util.function.Consumer;

import static io.mitallast.io.internals.TrampolineEC.immediate;
import static io.mitallast.kernel.Unit.unit;

public abstract class IOBracket {
    private IOBracket() {
    }

    /**
     * Implementation for `IO.bracketCase`.
     */
    public static <A, B> IO<B> apply(IO<A> acquire,
                                     Function1<A, IO<B>> use,
                                     Function2<A, ExitCase<Throwable>, IO<Unit>> release
    ) {
        return new IO.Async<>((conn, cb) -> IORunLoop.start(acquire, new BracketStart<>(use, release, conn, cb)));
    }

    /**
     * Implementation for `IO.guaranteeCase`.
     */
    public static <A> IO<A> guaranteeCase(IO<A> source, Function1<ExitCase<Throwable>, IO<Unit>> release) {
        return new IO.Async<>((conn, cb) -> immediate.execute(() -> {
            var frame = new EnsureReleaseFrame<A>(release, conn);
            var onNext = source.flatMap(frame);
            // Registering our cancelable token ensures that in case
            // cancellation is detected, `release` gets called
            conn.push(frame.cancel());
            // Actual execution
            IORunLoop.startCancelable(onNext, conn, cb);
        }));
    }

    private final static class BracketStart<A, B> implements Consumer<Either<Throwable, A>>, Runnable {
        private final Function1<A, IO<B>> use;
        private final Function2<A, ExitCase<Throwable>, IO<Unit>> release;
        private final IOConnection conn;
        private final Consumer<Either<Throwable, B>> cb;

        // This runnable is a dirty optimization to avoid some memory allocations;
        // This class switches from being a Callback to a Runnable, but relies on
        // the internal IO callback protocol to be respected (called at most once)
        private volatile Either<Throwable, A> result = null;

        private BracketStart(Function1<A, IO<B>> use, Function2<A, ExitCase<Throwable>, IO<Unit>> release, IOConnection conn, Consumer<Either<Throwable, B>> cb) {
            this.use = use;
            this.release = release;
            this.conn = conn;
            this.cb = cb;
        }

        @Override
        public void accept(Either<Throwable, A> ea) {
            if (result != null) {
                throw new IllegalStateException("callback called multiple times!");
            }
            // Introducing a light async boundary, otherwise executing the required
            // logic directly will yield a StackOverflowException
            result = ea;
            immediate.execute(this);
        }

        @Override
        public void run() {
            result.foreach(
                err -> cb.accept(Either.left(err)),
                this::onResult
            );
        }

        private void onResult(A a) {
            var frame = new BracketReleaseFrame<A, B>(a, release, conn);
            IO<B> fb;
            try {
                fb = use.apply(a);
            } catch (Exception e) {
                fb = IO.raiseError(e);
            }
            var onNext = fb.flatMap(frame);
            // Registering our cancelable token ensures that in case
            // cancellation is detected, `release` gets called
            conn.push(frame.cancel());
            // Actual execution
            IORunLoop.startCancelable(onNext, conn, cb);
        }
    }

    private static abstract class BaseReleaseFrame<A, B> extends IOFrame<B, IO<B>> {
        private final IOConnection conn;

        protected BaseReleaseFrame(IOConnection conn) {
            this.conn = conn;
        }

        public final IOConnection conn() {
            return conn;
        }

        public abstract IO<Unit> release(ExitCase<Throwable> c);

        public final IO<Unit> cancel() {
            return release(ExitCase.canceled()).uncancelable();
        }

        public final IO<B> recover(Throwable e) {
            // Unregistering cancel token, otherwise we can have a memory leak;
            // N.B. conn.pop() happens after the evaluation of `release`, because
            // otherwise we might have a conflict with the auto-cancellation logic
            return new IO.ContextSwitch<>(release(ExitCase.error(e)), makeUncancelable, disableUncancelableAndPop())
                .flatMap(new ReleaseRecover<>(e));
        }

        @Override
        public final IO<B> apply(final B b) {
            // Unregistering cancel token, otherwise we can have a memory leak
            // N.B. conn.pop() happens after the evaluation of `release`, because
            // otherwise we might have a conflict with the auto-cancellation logic
            return new IO.ContextSwitch<>(release(ExitCase.complete()), makeUncancelable, disableUncancelableAndPop())
                .map(a -> b);
        }
    }

    private static final class BracketReleaseFrame<A, B> extends BaseReleaseFrame<A, B> {
        private final A a;
        private final Function2<A, ExitCase<Throwable>, IO<Unit>> releaseFn;

        public BracketReleaseFrame(A a, Function2<A, ExitCase<Throwable>, IO<Unit>> releaseFn, IOConnection conn) {
            super(conn);
            this.a = a;
            this.releaseFn = releaseFn;
        }

        @Override
        public IO<Unit> release(ExitCase<Throwable> c) {
            return releaseFn.apply(a, c);
        }
    }

    private static final class EnsureReleaseFrame<A> extends BaseReleaseFrame<Unit, A> {
        private final Function1<ExitCase<Throwable>, IO<Unit>> releaseFn;

        public EnsureReleaseFrame(Function1<ExitCase<Throwable>, IO<Unit>> releaseFn, IOConnection conn) {
            super(conn);
            this.releaseFn = releaseFn;
        }

        @Override
        public IO<Unit> release(ExitCase<Throwable> c) {
            return releaseFn.apply(c);
        }
    }

    private static final class ReleaseRecover<A, B> extends IOFrame<A, IO<B>> {
        private final Throwable e;

        ReleaseRecover(Throwable e) {
            this.e = e;
        }

        @Override
        public IO<B> recover(Throwable err) {
            e.addSuppressed(err);
            return IO.raiseError(e);
        }

        @Override
        public IO<B> apply(A b) {
            return IO.raiseError(e);
        }
    }

    private static final Function1<IOConnection, IOConnection> makeUncancelable = conn -> IOConnection.uncancelable();

    private static <A> Function4<A, Throwable, IOConnection, IOConnection, IOConnection> disableUncancelableAndPop() {
        return (a, e, old, c) -> {
            old.pop();
            return old;
        };
    }
}
