package io.mitallast.higher;

import io.mitallast.lambda.Function1;

public interface Applicative<F extends Higher> extends Functor<F> {

    <A> Higher<F, A> pure(A a);

    <A, B> Higher<F, B> apply(Higher<F, Function1<A, B>> fm, Higher<F, A> fa);

    @Override
    default <A, B> Higher<F, B> map(Function1<A, B> fn, Higher<F, A> fa) {
        return apply(pure(fn), fa);
    }
}
