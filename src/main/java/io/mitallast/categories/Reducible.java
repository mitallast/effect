package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.kernel.Eval;
import io.mitallast.kernel.Semigroup;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Function2;

public interface Reducible<F extends Higher> extends Foldable<F> {
    default <A> A reduceLeft(Higher<F, A> fa, Function2<A, A, A> f) {
        return reduceLeftTo(fa, x -> x, f);
    }

    default <A> Eval<A> reduceRight(Higher<F, A> fa, Function2<A, Eval<A>, Eval<A>> f) {
        return reduceRightTo(fa, x -> x, f);
    }

    default <A> A reduce(Higher<F, A> fa, Semigroup<A> A) {
        return reduceLeft(fa, A::combine);
    }

    <A, B> B reduceLeftTo(Higher<F, A> fa, Function1<A, B> f, Function2<B, A, B> g);

    <A, B> Eval<B> reduceRightTo(Higher<F, A> fa, Function1<A, B> f, Function2<A, Eval<B>, Eval<B>> g);
}
