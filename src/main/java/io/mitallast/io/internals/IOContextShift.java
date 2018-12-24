package io.mitallast.io.internals;

import io.mitallast.concurrent.ExecutionContext;
import io.mitallast.higher.Higher;
import io.mitallast.io.ContextShift;
import io.mitallast.io.IO;
import io.mitallast.kernel.Unit;

public final class IOContextShift implements ContextShift<IO> {
    private final ExecutionContext context;

    private IOContextShift(ExecutionContext context) {
        this.context = context;
    }

    @Override
    public IO<Unit> shift() {
        return IOShift.apply(context);
    }

    @Override
    public <A> IO<A> evalOn(ExecutionContext target, Higher<IO, A> fa) {
        return IOShift.shiftOn(context, target, (IO<A>) fa);
    }

    public static ContextShift<IO> apply(ExecutionContext ec) {
        return new IOContextShift(ec);
    }
}
