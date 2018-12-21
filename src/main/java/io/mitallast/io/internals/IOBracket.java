package io.mitallast.io.internals;

import io.mitallast.either.Either;
import io.mitallast.io.ExitCase;
import io.mitallast.io.IO;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import static io.mitallast.io.internals.TrampolineEC.immediate;

public abstract class IOBracket {
    private IOBracket() {
    }

    public static <A, B> IO<B> apply(IO<A> acquire,
                                     Function1<A, IO<B>> use,
                                     BiFunction<A, ExitCase<Throwable>, IO<Unit>> release
    ) {
        new IO.Async<B>((conn, cb) -> IORunLoop.start(acquire, new BracketStart<>(use, release, conn, cb)));
    }

    private final static class BracketStart<A, B> implements Consumer<Either<Throwable, A>>, Runnable {
        private final Function1<A, IO<B>> use;
        private final BiFunction<A, ExitCase<Throwable>, IO<Unit>> release;
        private final IOConnection conn;
        private final Consumer<Either<Throwable, B>> cb;
        private volatile Either<Throwable, A> result = null;

        private BracketStart(Function1<A, IO<B>> use, BiFunction<A, ExitCase<Throwable>, IO<Unit>> release, IOConnection conn, Consumer<Either<Throwable, B>> cb) {
            this.use = use;
            this.release = release;
            this.conn = conn;
            this.cb = cb;
        }

        @Override
        public void accept(Either<Throwable, A> ea) {
            if(result != null) {
                throw new IllegalStateException("callback called multiple times!");
            }
            result = ea;
            immediate.execute(this);
        }

        @Override
        public void run() {
            result.fold(
                err -> {

                    return Unit.unit();
                },
                a -> {
                    var frame = new BracketReleaseFrame<>(a, release, conn);
                    return Unit.unit();
                }
            );
        }
    }

    private final class BracketReleaseFrame<A, B>() {

    }
}
