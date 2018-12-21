package io.mitallast.io.internals;

import io.mitallast.either.Either;
import io.mitallast.io.IO;

import java.util.function.Consumer;

public interface IORunLoop {
    static <A> void start(IO<A> source, Consumer<Either<Throwable, A>> cb) {
    }

    static <A> void startCancelable(IO<A> source, IOConnection connm, Consumer<Either<Throwable, A>> cb) {
    }
}
