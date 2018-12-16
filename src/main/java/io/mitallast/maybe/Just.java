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
    public <B> Maybe<B> flatMap(Function1<T, Monad<Maybe, B>> fn) {
        return (Maybe<B>) fn.apply(value);
    }

    @Override
    public String toString() {
        return "Just(" + value + ')';
    }
}
