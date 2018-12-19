package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.kernel.Semigroup;
import io.mitallast.lambda.Function1;

import java.util.function.BiFunction;

public interface Reducible<F extends Higher> extends Foldable<F> {
    default <A> A reduceLeft(Higher<F, A> fa, BiFunction<A, A, A> f) {
        return reduceLeftTo(fa, x -> x, f);
    }

    default <A> A reduceRight(Higher<F, A> fa, BiFunction<A, A, A> f) {
        return reduceRightTo(fa, x -> x, f);
    }

    default <A> A reduce(Higher<F, A> fa, Semigroup<A> A) {
        return reduceLeft(fa, A::combine);
    }

    <A, B> B reduceLeftTo(Higher<F, A> fa, Function1<A, B> f, BiFunction<B, A, B> g);

    <A, B> B reduceRightTo(Higher<F, A> fa, Function1<A, B> f, BiFunction<A, B, B> g);
}
