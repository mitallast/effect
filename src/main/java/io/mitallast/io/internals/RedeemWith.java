package io.mitallast.io.internals;

import io.mitallast.io.IO;
import io.mitallast.lambda.Function1;

public class RedeemWith<A, B> extends IOFrame<A, IO<B>> {
    private final Function1<Throwable, IO<B>> fe;
    private final Function1<A, IO<B>> fs;

    public RedeemWith(Function1<Throwable, IO<B>> fe, Function1<A, IO<B>> fs) {
        this.fe = fe;
        this.fs = fs;
    }

    @Override
    public IO<B> apply(A a) {
        return fs.apply(a);
    }

    @Override
    public IO<B> recover(Throwable e) {
        return fe.apply(e);
    }
}
