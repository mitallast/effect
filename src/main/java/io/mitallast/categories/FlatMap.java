package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

public interface FlatMap<F extends Higher> extends Apply<F> {
    <A, B> Higher<F, B> flatMap(Higher<F, A> fa, Function1<A, Higher<F, B>> f);

    default <A> Higher<F, A> flatten(Higher<F, Higher<F, A>> ffa) {
        return flatMap(ffa, fa -> fa);
    }
}
