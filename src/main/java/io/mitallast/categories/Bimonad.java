package io.mitallast.categories;

import io.mitallast.higher.Higher;

public interface Bimonad<F extends Higher> extends Monad<F>, Comonad<F> {
}
