package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

public interface Apply<F extends Higher> extends Functor<F>, Semigroupal<F> {
    <A, B> Higher<F, B> ap(Higher<F, Function1<A, B>> ff, Higher<F, A> fa);
}
