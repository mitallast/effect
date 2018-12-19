package io.mitallast.maybe;

import io.mitallast.lambda.Function1;

public class Nothing<T> extends Maybe<T> {
    public Nothing() {
    }

    @Override
    public <B> Maybe<B> flatMap(Function1<T, Maybe<B>> fn) {
        return new Nothing<>();
    }

    @Override
    public String toString() {
        return "Nothing";
    }
}
