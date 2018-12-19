package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

public interface CoflatMap<F extends Higher> extends Functor<F> {
    <A, B> Higher<F, B> coflatMap(Higher<F, A> fa, Function1<Higher<F, A>, B> f);

    default <A> Higher<F, Higher<F, A>> coflatten(Higher<F, A> fa) {
        return coflatMap(fa, f -> f);
    }
}
