package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

/**
 * [[ContravariantSemigroupal]] is nothing more than something both contravariant
 * and Semigroupal. It comes up enough to be useful, and composes well
 */
public interface ContravariantSemigroupal<F extends Higher> extends Invariant<F> {
    <A, B> Higher<F, B> contramap(Higher<F, A> fa, Function1<B, A> f);

    default <A, B> Higher<F, B> imap(Higher<F, A> fa, Function1<A, B> f, Function1<B, A> fi) {
        return contramap(fa, fi);
    }
}
