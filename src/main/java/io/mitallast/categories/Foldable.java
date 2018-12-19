package io.mitallast.categories;

import io.mitallast.higher.Higher;

import java.util.function.BiFunction;

public interface Foldable<F extends Higher> extends UnorderedFoldable<F> {
    <A, B> B foldLeft(Higher<F, A> fa, B b, BiFunction<B, A, B> f);

    <A, B> B foldRight(Higher<F, A> fa, B b, BiFunction<A, B, B> f);
}
