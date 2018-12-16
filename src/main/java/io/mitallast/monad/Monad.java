package io.mitallast.monad;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

public interface Monad<M extends Monad, A> extends Higher<M, A> {
    <B> Monad<M, B> flatMap(Function1<A, Monad<M, B>> fn);
}