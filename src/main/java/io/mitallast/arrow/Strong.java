package io.mitallast.arrow;

import io.mitallast.higher.Higher2;
import io.mitallast.product.Tuple2;

public interface Strong<F extends Higher2> extends Profunctor<F> {
    /**
     * Create a new `F` that takes two inputs, but only modifies the first input
     */
    <A, B, C> Higher2<F, Tuple2<A, C>, Tuple2<B, C>> first(Higher2<F, A, B> fab);

    /**
     * Create a new `F` that takes two inputs, but only modifies the second input
     */
    <A, B, C> Higher2<F, Tuple2<C, A>, Tuple2<C, B>> second(Higher2<F, A, B> fab);
}
