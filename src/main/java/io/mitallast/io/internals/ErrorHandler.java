package io.mitallast.io.internals;

import io.mitallast.io.IO;
import io.mitallast.lambda.Function1;

public final class ErrorHandler<A> extends IOFrame<A, IO<A>> {
    private final Function1<Throwable, IO<A>> fe;

    public ErrorHandler(Function1<Throwable, IO<A>> fe) {
        this.fe = fe;
    }

    @Override
    public IO<A> apply(A a) {
        return IO.pure(a);
    }

    @Override
    public IO<A> recover(Throwable e) {
        return fe.apply(e);
    }
}
