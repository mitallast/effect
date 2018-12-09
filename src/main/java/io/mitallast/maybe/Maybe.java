package io.mitallast.maybe;

import io.mitallast.higher.Higher;
import io.mitallast.monad.Monad;

public abstract class Maybe<T> implements Monad<T>, Higher<Maybe, T> {
    Maybe() {
    }
}
