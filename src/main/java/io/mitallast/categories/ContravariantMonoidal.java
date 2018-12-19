package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.kernel.Unit;

public interface ContravariantMonoidal<F extends Higher> extends InvariantMonoidal<F>, ContravariantSemigroupal<F> {

    default <A> Higher<F, A> trivial() {
        return contramap(unit(), a -> Unit.unit());
    }
}
