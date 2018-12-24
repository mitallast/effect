package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

public interface Traverse<F extends Higher> extends Functor<F>, Foldable<F>, UnorderedTraverse<F> {
    <G extends Higher, A, B>
    Higher<G, Higher<F, B>> traverse(Higher<F, A> fa, Function1<A, Higher<G, B>> f);

    <G extends Higher, A, B>
    Higher<G, Higher<F, B>> flatTraverse(Higher<F, A> fa, Function1<A, Higher<G, Higher<F, B>>> f);
}
