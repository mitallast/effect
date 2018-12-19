package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.kernel.Monoid;

public interface MonoidK<F extends Higher> extends SemigroupK<F> {
    <A> Higher<F, A> empty();

    @Override
    default <A> Monoid<Higher<F, A>> algebra() {
        return new Monoid<>() {
            @Override
            public Higher<F, A> empty() {
                return MonoidK.this.empty();
            }

            @Override
            public Higher<F, A> combine(Higher<F, A> x, Higher<F, A> y) {
                return MonoidK.this.combineK(x, y);
            }
        };
    }
}
