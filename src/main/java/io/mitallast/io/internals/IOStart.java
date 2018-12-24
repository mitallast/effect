package io.mitallast.io.internals;

import io.mitallast.either.Either;
import io.mitallast.io.ContextShift;
import io.mitallast.io.Fiber;
import io.mitallast.io.IO;

import java.util.concurrent.CompletableFuture;

public interface IOStart {

    /**
     * Implementation for `IO.start`.
     */
    static <A> IO<Fiber<IO, A>> apply(ContextShift<IO> cs, IO<A> fa) {
        return new IO.Async<>((conn, cb) -> {
            // Memoization
            var p = new CompletableFuture<Either<Throwable, A>>();

            // Starting the source `IO`, with a new connection, because its
            // cancellation is now decoupled from our current one
            var conn2 = IOConnection.apply();
            IORunLoop.startCancelable(IOForkedStart.apply(fa, cs), conn2, p::complete);

            cb.accept(Either.right(fiber(p, conn2)));
        }, true);
    }

    static <A> Fiber<IO, A> fiber(CompletableFuture<Either<Throwable, A>> p, IOConnection conn) {
        return Fiber.apply(retrow(IO.fromFuture(p)), conn.cancel());
    }

    static <A> IO<A> retrow(IO<Either<Throwable, A>> io) {
        return io.flatMap(e -> e.fold(IO::raiseError, IO::pure));
    }
}
