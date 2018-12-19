package io.mitallast.categories;

import io.mitallast.higher.Higher;

public interface Comonad<F extends Higher> extends CoflatMap<F> {
    <A> A extract(Higher<F, A> x);
}
