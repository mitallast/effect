package io.mitallast.maybe;

import io.mitallast.monad.MonadCompanion;

public class MaybeCompanion implements MonadCompanion<Maybe> {
    public final static MaybeCompanion instance = new MaybeCompanion();

    private MaybeCompanion() {
    }

    @Override
    public <T> Maybe<T> unit(T value) {
        if (value == null) {
            return new Nothing<>();
        } else {
            return new Just<>(value);
        }
    }
}
