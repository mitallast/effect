package io.mitallast.higher;

import io.mitallast.lambda.Function1;

public interface Functor<F extends Higher> {
    // fmap: (a->b)->fa->fb
    <A, B> Higher<F, B> map(Function1<A, B> fn, Higher<F, A> fa);
}
