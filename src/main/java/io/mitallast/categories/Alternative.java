package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.higher.Higher2;
import io.mitallast.product.Tuple2;

public interface Alternative<F extends Higher & Monad> extends Applicative<F>, MonoidK<F> {

    default <G extends Higher, A> Higher<F, A> unite(
        Higher<F, Higher<G, A>> fga,
        Monad<F> FM,
        Foldable<G> G
    ) {
        return FM.flatMap(fga, ga -> G.foldLeft(ga, empty(), (fa, a) -> combineK(fa, pure(a))));
    }

    default <G extends Higher2, A, B> Tuple2<Higher<F, A>, Higher<F, B>> separate(
        Higher<F, Higher2<G, A, B>> fgab,
        Monad<F> FM, BiFoldable<G> G
    ) {
        Higher<F, A> as = FM.flatMap(fgab, gab -> G.bifoldMap(gab, a -> pure(a), b -> empty(), algebra()));
        Higher<F, B> bs = FM.flatMap(fgab, gab -> G.bifoldMap(gab, a -> empty(), b -> pure(b), algebra()));
        return new Tuple2<>(as, bs);
    }
}
