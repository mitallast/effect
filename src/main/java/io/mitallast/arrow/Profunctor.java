package io.mitallast.arrow;

import io.mitallast.higher.Higher2;
import io.mitallast.lambda.Function1;

public interface Profunctor<F extends Higher2> {

    /**
     * Contramap on the first type parameter and map on the second type parameter
     */
    <A, B, C, D> Higher2<F, C, D> dimap(Higher2<F, A, B> fab,
                                        Function1<C, A> f,
                                        Function1<B, D> g);

    /**
     * contramap on the first type parameter
     */
    default <A, B, C> Higher2<F, C, B> lmap(Higher2<F, A, B> fab,
                                            Function1<C, A> f) {
        return dimap(fab, f, b -> b);
    }

    /**
     * map on the second type parameter
     */
    default <A, B, C> Higher2<F, A, C> rmap(Higher2<F, A, B> fab,
                                            Function1<B, C> f) {
        return dimap(fab, a -> a, f);
    }
}
