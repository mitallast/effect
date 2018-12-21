package io.mitallast.io;

import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;

/**
 * A monad that can suspend side effects into the `F` context and
 * that supports lazy and potentially asynchronous evaluation.
 * <p>
 * This type class is describing data types that:
 * <p>
 * 1. implement the [[Async]] algebra
 * 1. implement a lawful [[Effect!.runAsync runAsync]] operation
 * that triggers the evaluation (in the context of [[IO]])
 * <p>
 * Note this is the safe and generic version of [[IO.unsafeRunAsync]]
 * (aka Haskell's `unsafePerformIO`).
 */
public interface Effect<F extends Higher> extends Async<F> {
    /**
     * Evaluates `F[_]`, with the effect of starting the run-loop
     * being suspended in the `SyncIO` context.
     * <p>
     * {{{
     * val io = F.runAsync(fa)(cb)
     * // Running io results in evaluation of `fa` starting
     * io.unsafeRunSync
     * }}}
     */
    <A> SyncIO<A> runAsync(Higher<F, A> fa, Function1<Either<Throwable, A>, IO<Unit>> cb);

    default <A> IO<A> toIO(Higher<F, A> fa) {
        // @todo implement
        return null;
    }
}
