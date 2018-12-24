package io.mitallast.io.internals;

import io.mitallast.io.IO;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Function4;

public class IOCancel {
    public static <A> IO<A> uncancelable(IO<A> fa) {
        return new IO.ContextSwitch<>(fa, makeUncancelable, disableUncancelable());
    }

    private static final Function1<IOConnection, IOConnection> makeUncancelable = conn -> IOConnection.uncancelable();

    private static <A> Function4<A, Throwable, IOConnection, IOConnection, IOConnection> disableUncancelable() {
        return (a, e, old, c) -> old;
    }
}
