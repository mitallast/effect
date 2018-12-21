package io.mitallast.io;

import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.io.internals.*;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Function4;
import io.mitallast.maybe.Maybe;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class IO<A> implements Higher<IO, A> {
    private IO() {
    }

    public <B> IO<B> map(Function1<A, B> f) {
        return new Map<>(this, f, 0);
    }

    public <B> IO<B> flatMap(Function1<A, IO<B>> f) {
        return new Bind<>(this, f);
    }

    public IO<Either<Throwable, A>> attempt() {
        return new Bind<>(this, new AttemptIO<>());
    }

    public final SyncIO<Unit> runAsync(Function1<Either<Throwable, A>, IO<Unit>> cb) {
        return null; // @todo implement
    }

    public final SyncIO<IO<Unit>> runCancelable(Function1<Either<Throwable, A>, IO<Unit>> cb) {
        return null; // @todo implement
    }

    public final A unsafeRunSync() {
        return null; // @todo implement
    }

    public final void unsafeRunAsync(Consumer<Either<Throwable, A>> cb) {
        IORunLoop.start(this, cb);
    }

    public final void unsafeRunAsyncAndForget() {
        unsafeRunAsync(Callback.report());
    }

    public final IO<Unit> unsafeRunCancelable(Consumer<Either<Throwable, A>> cb) {
        var conn = IOConnection.apply();
        IORunLoop.startCancelable(this, conn, cb);
        return conn.cancel();
    }

    public final Maybe<A> unsafeRunTimed(Duration limit) {
        return null; // @todo implement
    }

    public final CompletableFuture<A> unsafeToFuture() {
        var p = new CompletableFuture<A>();
        unsafeRunAsync(e -> e.fold(p::completeExceptionally, p::complete));
        return p;
    }

    public final IO<Fiber<IO, A>> start() {
        return null; // @todo implement
    }

    public final IO<A> uncancelable() {
        return null; // @todo implement
    }

    public final <F extends Higher> Higher<F, A> to(LiftIO<F> F) {
        return F.liftIO(this);
    }

    public final IO<A> timeoutTo(Duration duration, IO<A> fallback) {
        return null; // @todo implement
    }

    public final IO<A> timeout(Duration duration) {
        return null; // @todo implement
    }

    public final <B> IO<B> bracket(Function1<A, IO<B>> use, Function1<A, IO<Unit>> release) {
        return null; // @todo implement
    }

    public final <B> IO<B> bracketCase(Function1<A, IO<B>> use, BiFunction<A, ExitCase<Throwable>, IO<Unit>> release) {
        return null; // @todo implement
    }

    public final IO<A> guarantee(IO<Unit> finalizer) {
        return null; // @todo implement
    }

    public final IO<A> guaranteeCase(Function1<ExitCase<Throwable>, IO<Unit>> finalizer) {
        return null; // @todo implement
    }

    public final IO<A> handleErrorWith(Function1<Throwable, IO<A>> f) {
        return new Bind<>(this, new ErrorHandler<>(f));
    }

    public final <B> IO<B> redeem(Function1<Throwable, B> recover, Function1<A, B> map) {
        return new Bind<>(this, new Redeem<>(recover, map));
    }

    public final <B> IO<B> redeemWith(Function1<Throwable, IO<B>> recover, Function1<A, IO<B>> map) {
        return new Bind<>(this, new RedeemWith<>(recover, map));
    }

    public static <A> IO<A> apply(Supplier<A> thunk) {
        return delay(thunk);
    }

    public static <A> IO<A> delay(Supplier<A> thunk) {
        return new Delay<>(thunk);
    }

    public static <A> IO<A> suspend(Supplier<IO<A>> thunk) {
        return new Suspend<>(thunk);
    }

    public static <A> IO<A> pure(A a) {
        return new Pure<>(a);
    }

    public static IO<Unit> unit() {
        return new Pure<>(Unit.unit());
    }

    public static <A> IO<A> async(Consumer<Consumer<Either<Throwable, A>>> k) {
        return new Async<>((conn, cb) -> {
            var cb2 = Callback.asyncIdempotent(null, cb);
            try {
                k.accept(cb2);
            } catch (Exception e) {
                cb2.accept(Either.left(e));
            }
        });
    }

    public static <A> IO<A> asyncF(Function1<Consumer<Either<Throwable, A>>, IO<Unit>> k) {
        return new Async<>((conn, cb) -> {
            var conn2 = IOConnection.apply();
            var cb2 = Callback.asyncIdempotent(null, cb);
            conn.push(conn2.cancel());
            IO<Unit> fa;
            try {
                fa = k.apply(cb2);
            } catch (Exception e) {
                fa = IO.apply(() -> {
                    cb2.accept(Either.left(e));
                    return Unit.unit();
                });
            }
            IORunLoop.startCancelable(fa, conn2, Callback.report());
        });
    }

    public static <A> IO<A> cancelable(Function1<Consumer<Either<Throwable, A>>, IO<Unit>> k) {
        return new Async<>((conn, cb) -> {
            var cb2 = Callback.asyncIdempotent(null, cb);
            var ref = ForwardCancelable.apply();
            conn.push(ref.cancel());
            try {
                ref.set(k.apply(cb2));
            } catch (Exception e) {
                cb2.accept(Either.left(e));
                ref.set(IO.unit());
            }
        });
    }

    public static <A> IO<A> raiseError(Throwable e) {
        return new RaiseError<>(e);
    }

    public static <A> IO<A> fromFuture(CompletableFuture<A> promise) {
        return async(cb -> {
            promise.whenComplete((a, e) -> {
                if (e != null) {
                    cb.accept(Either.left(e));
                } else {
                    cb.accept(Either.right(a));
                }
            });
        });
    }

    public static <A> IO<A> fromFuture(IO<CompletableFuture<A>> iof) {
        return iof.flatMap(IO::fromFuture);
    }

    public static <A> IO<A> fromEither(Either<Throwable, A> e) {
        return e.fold(IO::raiseError, IO::pure);
    }

    public static final class Pure<A> extends IO<A> {
        private final A a;

        public Pure(A a) {
            this.a = a;
        }

        public A a() {
            return a;
        }
    }

    public static final class Delay<A> extends IO<A> {
        private final Supplier<A> thunk;

        public Delay(Supplier<A> thunk) {
            this.thunk = thunk;
        }

        public Supplier<A> thunk() {
            return thunk;
        }
    }

    public static final class RaiseError<A> extends IO<A> {
        private final Throwable e;

        public RaiseError(Throwable e) {
            this.e = e;
        }

        public Throwable e() {
            return e;
        }
    }

    public static final class Suspend<A> extends IO<A> {
        private final Supplier<IO<A>> thunk;

        public Suspend(Supplier<IO<A>> thunk) {
            this.thunk = thunk;
        }

        public Supplier<IO<A>> thunk() {
            return thunk;
        }
    }

    public static final class Bind<E, A> extends IO<A> {
        private final IO<E> source;
        private final Function1<E, IO<A>> f;

        public Bind(IO<E> source, Function1<E, IO<A>> f) {
            this.source = source;
            this.f = f;
        }

        public IO<E> source() {
            return source;
        }

        public Function1<E, IO<A>> f() {
            return f;
        }
    }

    public static final class Map<E, A> extends IO<A> {
        private final IO<E> source;
        private final Function1<E, A> f;
        private final int index;

        public Map(IO<E> source, Function1<E, A> f, int index) {
            this.source = source;
            this.f = f;
            this.index = index;
        }

        @Override
        public <B> IO<B> map(Function1<A, B> f) {
            // @todo fusionMaxStackDepth
            return new Map<>(this, f, index + 1);
        }

        public IO<E> source() {
            return source;
        }

        public Function1<E, A> f() {
            return f;
        }

        public int index() {
            return index;
        }
    }

    public static final class Async<A> extends IO<A> {
        private final BiConsumer<IOConnection, Consumer<Either<Throwable, A>>> k;
        private final boolean trampolineAfter;

        public Async(BiConsumer<IOConnection, Consumer<Either<Throwable, A>>> k) {
            this(k, false);
        }

        public Async(BiConsumer<IOConnection, Consumer<Either<Throwable, A>>> k, boolean trampolineAfter) {
            this.k = k;
            this.trampolineAfter = trampolineAfter;
        }

        public BiConsumer<IOConnection, Consumer<Either<Throwable, A>>> k() {
            return k;
        }

        public boolean trampolineAfter() {
            return trampolineAfter;
        }

    }

    public static final class ContextSwitch<A> extends IO<A> {
        private final IO<A> source;
        private final Function1<IOConnection, IOConnection> modify;
        private final Function4<A, Throwable, IOConnection, IOConnection, IOConnection> restore;

        public ContextSwitch(IO<A> source, Function1<IOConnection, IOConnection> modify, Function4<A, Throwable, IOConnection, IOConnection, IOConnection> restore) {
            this.source = source;
            this.modify = modify;
            this.restore = restore;
        }

        public IO<A> source() {
            return source;
        }

        public Function1<IOConnection, IOConnection> modify() {
            return modify;
        }

        public Function4<A, Throwable, IOConnection, IOConnection, IOConnection> restore() {
            return restore;
        }
    }
}

