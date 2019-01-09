package io.mitallast.arrow;

import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.higher.Higher2;

public interface Choice<F extends Higher2> extends Category<F> {
    /**
     * Given two `F`s (`f` and `g`) with a common target type, create a new `F`
     * with the same target type, but with a source type of either `f`'s source
     * type OR `g`'s source type.
     */
    <A, B, C> Higher2<F, Either<A, B>, C> choice(Higher2<F, A, C> f,
                                                 Higher2<F, B, C> g);

    /**
     * An `F` that, given a source `A` on either the right or left side, will
     * return that same `A` object.
     */
    default <A> Higher2<F, Either<A, A>, A> codiagonal() {
        return choice(id(), id());
    }
}
