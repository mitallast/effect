package io.mitallast.higher;

import io.mitallast.lambda.Function1;

public interface Functor<A, B, F extends Higher, FA extends Higher<F, A>, FB extends Higher<F, B>> {
    FB map(Function1<A, B> fn, FA fa);
}
