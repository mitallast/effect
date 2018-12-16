package io.mitallast.maybe;

import io.mitallast.lambda.Function1;
import io.mitallast.monad.Monad;

public class Nothing<T> extends Maybe<T> {
    public Nothing() {
    }

    @Override
    public <B> Maybe<B> flatMap(Function1<T, Monad<Maybe, B>> fn) {
        return new Nothing<>();
    }

    @Override
    public String toString() {
        return "Nothing";
    }
}
