package io.mitallast.monad;

public interface MonadCompanion<M extends Monad> {
    <T> Monad<M, T> unit(T value);
}
