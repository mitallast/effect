package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

public interface Functor<F extends Higher> extends Invariant<F> {
    <A, B> Higher<F, B> map(Higher<F, A> fa, Function1<A, B> fn);

    default <A, B> Higher<F, B> imap(Higher<F, A> fa, Function1<A, B> f, Function1<B, A> g) {
        return map(fa, f);
    }
}
