package io.mitallast.monad;

import io.mitallast.higher.Higher;

public interface MonadCompanion<T, F extends Monad, FT extends Monad<T> & Higher<F, T>> {
    FT unit(T value);
}
