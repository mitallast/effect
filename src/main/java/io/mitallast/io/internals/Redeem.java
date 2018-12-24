package io.mitallast.io.internals;

import io.mitallast.io.IO;
import io.mitallast.lambda.Function1;

public class Redeem<A, B> extends IOFrame<A, IO<B>> {
    private final Function1<Throwable, B> fe;
    private final Function1<A, B> fs;

    public Redeem(Function1<Throwable, B> fe, Function1<A, B> fs) {
        this.fe = fe;
        this.fs = fs;
    }

    @Override
    public IO<B> apply(A a) {
        return IO.pure(fs.apply(a));
    }

    @Override
    public IO<B> recover(Throwable e) {
        return IO.pure(fe.apply(e));
    }
}
