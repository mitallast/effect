package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;

public interface Applicative<F extends Higher> extends Apply<F> {
    <A> Higher<F, A> pure(A x);

    default Higher<F, Unit> unit() {
        return pure(Unit.unit());
    }

    default <A, B> Higher<F, B> map(Higher<F, A> fa, Function1<A, B> f) {
        return ap(pure(f), fa);
    }
}
