package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.kernel.Semigroup;

public interface SemigroupK<F extends Higher> {
    <A> Higher<F, A> combineK(Higher<F, A> x, Higher<F, A> y);

    default <A> Semigroup<Higher<F, A>> algebra() {
        return this::combineK;
    }
}
