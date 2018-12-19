package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

/**
 * `UnorderedFoldable` is like a `Foldable` for unordered containers.
 */
public interface UnorderedFoldable<F extends Higher> {
    <A, B> B unorderedFoldMap(Higher<F, A> fa, Function1<A, B> f);

    default <A> A unorderedFold(Higher<F, A> fa) {
        return unorderedFoldMap(fa, x -> x);
    }
}
