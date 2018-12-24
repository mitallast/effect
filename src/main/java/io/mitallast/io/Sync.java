package io.mitallast.io;

import io.mitallast.lambda.Supplier;
import io.mitallast.higher.Higher;

/**
 * A monad that can suspend the execution of side effects
 * in the `F[_]` context.
 */
public interface Sync<F extends Higher> extends Bracket<F, Throwable>, Defer<F> {
    /**
     * Suspends the evaluation of an `F` reference.
     * <p>
     * Equivalent to `FlatMap.flatten` for pure expressions,
     * the purpose of this function is to suspend side effects
     * in `F`.
     */
    <A> Higher<F, A> suspend(Supplier<Higher<F, A>> thunk);

    /**
     * Alias for `suspend` that suspends the evaluation of
     * an `F` reference and implements `cats.Defer` typeclass.
     */
    @Override
    default <A> Higher<F, A> defer(Supplier<Higher<F, A>> fa) {
        return suspend(fa);
    }

    default <A> Higher<F, A> delay(Supplier<A> thunk) {
        return suspend(() -> pure(thunk.get()));
    }
}
