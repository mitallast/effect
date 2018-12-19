package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

public interface Monad<F extends Higher> extends FlatMap<F>, Applicative<F> {
    default <A, B> Higher<F, B> map(Higher<F, A> fa, Function1<A, B> f) {
        return flatMap(fa, a -> pure(f.apply(a)));
    }
}
