package io.mitallast.io.internals;

import io.mitallast.either.Either;
import io.mitallast.io.ContextShift;
import io.mitallast.io.Fiber;
import io.mitallast.io.IO;
import io.mitallast.kernel.Unit;
import io.mitallast.product.Tuple2;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public interface IORace {

    private static <T, U> void onSuccess(
        AtomicBoolean isActive,
        IOConnection main,
        IOConnection other,
        Consumer<Either<Throwable, Either<T, U>>> cb,
        Either<T, U> r
    ) {
        if (isActive.getAndSet(false)) {
            // First interrupts the other task
            other.cancel().unsafeRunAsync(r2 -> {
                main.pop();
                cb.accept(Either.right(r));
                maybeReport(r2);
            });
        }
    }

    private static <T> void onError(
        AtomicBoolean isActive,
        Consumer<Either<Throwable, T>> cb,
        IOConnection main,
        IOConnection other,
        Throwable err
    ) {
        if (isActive.getAndSet(false)) {
            other.cancel().unsafeRunAsync(r2 -> {
                main.pop();
                cb.accept(Either.left(composeErrors(err, r2)));
            });
        } else {
            IOLogger.reportFailure(err);
        }
    }

    static <A, B> IO<Either<A, B>> simple(ContextShift<IO> cs, IO<A> lh, IO<B> rh) {
        return new IO.Async<>((conn, cb) -> {
            var active = new AtomicBoolean(true);
            var connL = IOConnection.apply();
            var connR = IOConnection.apply();
            conn.pushPair(connL, connR);

            IORunLoop.startCancelable(IOForkedStart.apply(lh, cs), connL, e -> e.foreach(
                err -> onError(active, cb, conn, connR, err),
                a -> onSuccess(active, conn, connR, cb, Either.left(a))
            ));

            IORunLoop.startCancelable(IOForkedStart.apply(rh, cs), connR, e -> e.foreach(
                err -> onError(active, cb, conn, connL, err),
                a -> onSuccess(active, conn, connL, cb, Either.right(a))
            ));
        }, true);
    }

    static <A, B>
    IO<
        Either<
            Tuple2<A, Fiber<IO, B>>,
            Tuple2<Fiber<IO, A>, B>>> pair(ContextShift<IO> cs, IO<A> lh, IO<B> rh) {

        return new IO.Async<>((conn, cb) -> {
            var active = new AtomicBoolean(true);
            // Cancelable connection for the left value
            var connL = IOConnection.apply();
            var promiseL = new CompletableFuture<Either<Throwable, A>>();
            // Cancelable connection for the right value
            var connR = IOConnection.apply();
            var promiseR = new CompletableFuture<Either<Throwable, B>>();

            // Registers both for cancellation — gets popped right
            // before callback is invoked in onSuccess / onError
            conn.pushPair(connL, connR);

            IORunLoop.startCancelable(IOForkedStart.apply(lh, cs), connL, e -> e.foreach(
                err -> {
                    if (active.getAndSet(false)) {
                        connR.cancel().unsafeRunAsync(r2 -> {
                            conn.pop();
                            cb.accept(Either.left(composeErrors(err, r2)));
                        });
                    } else {
                        promiseL.complete(Either.left(err));
                    }
                },
                a -> {
                    if (active.getAndSet(false)) {
                        conn.pop();
                        cb.accept(Either.right(Either.left(new Tuple2<>(a, IOStart.fiber(promiseR, connR)))));
                    } else {
                        promiseL.complete(Either.right(a));
                    }
                }
            ));
            IORunLoop.startCancelable(IOForkedStart.apply(rh, cs), connR, e -> e.foreach(
                err -> {
                    if (active.getAndSet(false)) {
                        connL.cancel().unsafeRunAsync(r2 -> {
                            conn.pop();
                            cb.accept(Either.left(composeErrors(err, r2)));
                        });
                    } else {
                        promiseR.complete(Either.left(err));
                    }
                },
                b -> {
                    if (active.getAndSet(false)) {
                        conn.pop();
                        cb.accept(Either.right(Either.right(new Tuple2<>(IOStart.fiber(promiseL, connL), b))));
                    } else {
                        promiseR.complete(Either.right(b));
                    }
                }
            ));
        }, true);
    }

    private static <A> void maybeReport(Either<Throwable, A> r) {
        r.foreach(IOLogger::reportFailure, a -> {
        });
    }

    private static <A> Throwable composeErrors(Throwable e, Either<Throwable, A> r) {
        return r.fold(
            e2 -> IOPlatform.composeErrors(e, e2),
            a -> e
        );
    }
}
