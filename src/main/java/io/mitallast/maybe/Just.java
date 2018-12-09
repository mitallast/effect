package io.mitallast.maybe;

import io.mitallast.lambda.Function1;
import io.mitallast.monad.Monad;

public class Just<T> extends Maybe<T> {
    private final T value;

    Just(T value) {
        this.value = value;
    }

    public T value() {
        return value;
    }

    @Override
    public <B, MB extends Monad<B>> MB flatMap(Function1<T, MB> fn) {
        return fn.apply(this.value);
    }

    @Override
    public String toString() {
        return "Just(" + value + ')';
    }
}
