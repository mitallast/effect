package io.mitallast.maybe;

import io.mitallast.lambda.Function1;

public class Just<T> extends Maybe<T> {
    private final T value;

    Just(T value) {
        this.value = value;
    }

    public T value() {
        return value;
    }

    @Override
    public <B> Maybe<B> flatMap(Function1<T, Maybe<B>> fn) {
        return fn.apply(value);
    }

    @Override
    public String toString() {
        return "Just(" + value + ')';
    }
}
