package io.mitallast.arrow;

import io.mitallast.higher.Higher2;
import io.mitallast.lambda.Function1;
import io.mitallast.product.Tuple;
import io.mitallast.product.Tuple2;

public interface Arrow<F extends Higher2> extends Category<F>, Strong<F> {

    /**
     * Lift a function into the context of an Arrow.
     * <p>
     * In the reference articles "Arrows are Promiscuous...", and in the corresponding Haskell
     * library `Control.Arrow`, this function is called `arr`.
     */
    <A, B> Higher2<F, A, B> lift(Function1<A, B> f);

    @Override
    default <A> Higher2<F, A, A> id() {
        return lift(a -> a);
    }

    @Override
    default <A, B, C, D> Higher2<F, C, D> dimap(Higher2<F, A, B> fab,
                                                Function1<C, A> f,
                                                Function1<B, D> g) {
        return compose(lift(g), andThen(lift(f), fab));
    }

    default <X, Y> Higher2<F, Tuple2<X, Y>, Tuple2<Y, X>> swap() {
        return lift(xy -> Tuple.of(xy.t2(), xy.t1()));
    }

    @Override
    default <A, B, C> Higher2<F, Tuple2<C, A>, Tuple2<C, B>> second(Higher2<F, A, B> fa) {
        return compose(swap(), compose(first(fa), swap()));
    }

    /**
     * Create a new computation `F` that splits its input between `f` and `g`
     * and combines the output of each.
     */
    default <A, B, C, D> Higher2<F, Tuple2<A, C>, Tuple2<B, D>> split(Higher2<F, A, B> f,
                                                                      Higher2<F, C, D> g) {
        return andThen(first(f), second(g));
    }

    /**
     * Create a new computation `F` that merge outputs of `f` and `g` both having the same input
     */
    default <A, B, C> Higher2<F, A, Tuple2<B, C>> merge(Higher2<F, A, B> f,
                                                        Higher2<F, A, C> g) {
        return andThen(lift(x -> Tuple.of(x, x)), split(f, g));
    }
}
