package io.mitallast.io.internals;

import io.mitallast.either.Either;
import io.mitallast.io.IO;
import io.mitallast.lambda.Function1;

abstract class IOFrame<A, R> implements Function1<A, R> {
    @Override
    abstract public R apply(A a);

    abstract public R recover(Throwable e);

    final public R fold(Either<Throwable, A> value) {
        return value.fold(this::recover, this);
    }
}