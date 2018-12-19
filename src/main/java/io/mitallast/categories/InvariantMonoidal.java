package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.kernel.Unit;

public interface InvariantMonoidal<F extends Higher> extends InvariantSemigroupal<F> {
    default <A> Higher<F, A> point(A a) {
        return imap(unit(), u -> a, b -> Unit.unit());
    }

    Higher<F, Unit> unit();
}
