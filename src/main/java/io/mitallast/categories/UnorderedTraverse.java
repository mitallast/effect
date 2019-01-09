package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

// level 1
public interface UnorderedTraverse<F extends Higher> extends UnorderedFoldable<F> {
    <G extends Higher, A, B>
    Higher<G, Higher<F, B>> unorderedTraverse(Higher<F, A> sa,
                                              Function1<A, Higher<G, B>> f,
                                              CommutativeApplicative<G> G);

    default <G extends Higher, A>
    Higher<G, Higher<F, A>> unorderedSequence(Higher<F, Higher<G, A>> fga,
                                              CommutativeApplicative<G> G) {
        return unorderedTraverse(fga, x -> x, G);
    }
}
