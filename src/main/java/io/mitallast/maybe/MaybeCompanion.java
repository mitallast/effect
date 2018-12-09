package io.mitallast.maybe;

import io.mitallast.monad.MonadCompanion;

public class MaybeCompanion<T> implements MonadCompanion<T, Maybe, Maybe<T>> {
    @Override
    public Maybe<T> unit(T value) {
        if (value == null) {
            return new None<>();
        } else {
            return new Just<>(value);
        }
    }
}
