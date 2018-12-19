package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

public interface Distributive<F extends Higher> extends Functor {
    <G extends Functor & Higher, A, B>
    Higher<F, Higher<G, B>> distribute(Higher<G, A> fa, Function1<A, Higher<F, B>> f);

    default <G extends Functor & Higher, A>
    Higher<F, Higher<G, A>> cosequence(Higher<G, Higher<F, A>> ga) {
        return distribute(ga, x -> x);
    }
}
