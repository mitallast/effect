package io.mitallast.io.internals;

import io.mitallast.io.IO;
import io.mitallast.kernel.Unit;

public abstract class IOConnection {
    private IOConnection() {
    }

    abstract public IO<Unit> cancel();

    abstract public boolean isCanceled();

    abstract public void push(IO<Unit> token);

    abstract public void pushPair(IOConnection lh, IOConnection rh);

    abstract public IO<Unit> pop();

    abstract public boolean tryReactivate();

    public static IOConnection apply() {
        return null; // todo implement
    }
}
