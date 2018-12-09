package io.mitallast.maybe;

import io.mitallast.lambda.Function1;
import io.mitallast.monad.Monad;

public class None<T> extends Maybe<T> {
    public None() {
    }

    @Override
    public <B, MB extends Monad<B>> MB flatMap(Function1<T, MB> fn) {
        return (MB) new None<B>();
    }

    @Override
    public String toString() {
        return "None";
    }
}
