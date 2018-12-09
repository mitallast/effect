package io.mitallast.monad;

import io.mitallast.lambda.Function1;

public interface Monad<A> {
    <B, MB extends Monad<B>> MB flatMap(Function1<A, MB> fn);
}