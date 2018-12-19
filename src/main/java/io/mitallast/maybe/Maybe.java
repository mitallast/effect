package io.mitallast.maybe;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

public abstract class Maybe<T> implements Higher<Maybe, T> {
    Maybe() {
    }

    abstract public <B> Maybe<B> flatMap(Function1<T, Maybe<B>> fn);

    public static <T> Maybe<T> apply(T value) {
        if (value == null) {
            return new Nothing<>();
        } else {
            return new Just<>(value);
        }
    }
}
