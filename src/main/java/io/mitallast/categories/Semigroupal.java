package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.product.Tuple2;

public interface Semigroupal<F extends Higher> {
    <A, B> Higher<F, Tuple2<A, B>> product(Higher<F, A> fa, Higher<F, B> fb);
}
