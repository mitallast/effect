package io.mitallast.io;

import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;

public interface ConcurrentEffect<F extends Higher> extends Concurrent<F>, Effect<F> {
    /**
     * Evaluates `F[_]` with the ability to cancel it.
     * <p>
     * The returned `SyncIO[CancelToken[F]]` is a suspended cancelable
     * action that can be used to cancel the running computation.
     * <p>
     * [[CancelToken]] is nothing more than an alias for `F[Unit]`
     * and needs to be evaluated in order for cancelation of the
     * active process to occur.
     * <p>
     * Contract:
     * <p>
     * - the evaluation of the suspended [[CancelToken]] must be asynchronous
     */
    <A> SyncIO<A> runCancelable(Higher<F, A> fa, Function1<Either<Throwable, A>, IO<Unit>> cb);

    @Override
    default <A> IO<A> toIO(Higher<F, A> fa) {
        // @todo implement
        return null;
    }
}
