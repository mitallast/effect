package io.mitallast.io.internals;

import io.mitallast.concurrent.ExecutionContext;
import io.mitallast.either.Either;
import io.mitallast.io.IO;
import io.mitallast.kernel.Unit;

import java.util.function.Consumer;

public interface IOShift {
    static IO<Unit> apply(ExecutionContext ec) {
        return new IO.Async<>(new IOForkedStart<>() {
            @Override
            public void accept(IOConnection conn, Consumer<Either<Throwable, Unit>> cb) {
                ec.execute(new Tick(cb));
            }
        });
    }

    static <A> IO<A> shiftOn(ExecutionContext cs, ExecutionContext targetEc, IO<A> io) {
        return IOBracket.apply(apply(cs), u -> io, (u, e) -> apply(targetEc));
    }
}

final class Tick implements Runnable {
    private final Consumer<Either<Throwable, Unit>> cb;

    Tick(Consumer<Either<Throwable, Unit>> cb) {
        this.cb = cb;
    }

    @Override
    public void run() {
        cb.accept(Either.right(Unit.unit()));
    }
}
