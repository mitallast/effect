package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function2;

public interface Foldable<F extends Higher> extends UnorderedFoldable<F> {
    <A, B> B foldLeft(Higher<F, A> fa, B b, Function2<B, A, B> f);

    <A, B> B foldRight(Higher<F, A> fa, B b, Function2<A, B, B> f);
}
