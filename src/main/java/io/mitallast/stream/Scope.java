package io.mitallast.stream;

import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.kernel.Unit;
import io.mitallast.maybe.Maybe;

abstract class Scope<F extends Higher> {
    public abstract Higher<F, Maybe<Scope.Lease<F>>> lease();

    public abstract Higher<F, Unit> interrupt(Either<Throwable, Unit> cause);

    abstract static class Lease<F extends Higher> {
        public abstract Higher<F, Either<Throwable, Unit>> cancel();
    }
}
