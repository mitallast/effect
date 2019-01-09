package io.mitallast.arrow;

import io.mitallast.either.Either;
import io.mitallast.higher.Higher2;

public interface ArrowChoice<F extends Higher2> extends Arrow<F>, Choice<F> {

    /**
     * ArrowChoice yields Arrows with choice, allowing distribution
     * over coproducts.
     * <p>
     * Given two `F`s (`f` and `g`), create a new `F` with
     * domain the coproduct of the domains of `f` and `g`,
     * and codomain the coproduct of the codomains of `f` and `g`.
     * This is the sum notion to `split`'s product.
     */
    <A, B, C, D> Higher2<F, Either<A, B>, Either<C, D>> choose(Higher2<F, A, C> f,
                                                               Higher2<F, B, D> g);

    default <A, B, C> Higher2<F, Either<A, C>, Either<B, C>> left(Higher2<F, A, B> fab) {
        return choose(fab, lift(c -> c));
    }

    default <A, B, C> Higher2<F, Either<C, A>, Either<C, B>> right(Higher2<F, A, B> fab) {
        return choose(lift(c -> c), fab);
    }

    @Override
    default <A, B, C> Higher2<F, Either<A, B>, C> choice(Higher2<F, A, C> f, Higher2<F, B, C> g) {
        return rmap(choose(f, g), c -> c.fold(x -> x, x -> x));
    }
}
