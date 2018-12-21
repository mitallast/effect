package io.mitallast.io.internals;

import io.mitallast.either.Either;
import io.mitallast.io.IO;

public class AttemptIO<A> extends IOFrame<A, IO<Either<Throwable, A>>> {

    @Override
    public IO<Either<Throwable, A>> apply(A a) {
        return IO.pure(Either.right(a));
    }

    @Override
    public IO<Either<Throwable, A>> recover(Throwable e) {
        return IO.pure(Either.left(e));
    }
}
