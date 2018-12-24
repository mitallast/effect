package io.mitallast;

import io.mitallast.concurrent.ExecutionContext;
import io.mitallast.io.IO;
import io.mitallast.kernel.Unit;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class IOExample {
    public static void main(String... args) {

        var ec = new ExecutionContext() {
            final Executor executor = Executors.newSingleThreadExecutor();

            @Override
            public void execute(Runnable runnable) {
                executor.execute(runnable);
            }

            @Override
            public void reportFailure(Throwable cause) {
                cause.printStackTrace();
            }
        };

        var result = IO.pure(123)
            .flatMap(i -> IO.shift(ec).map(u -> i))
            .map(i -> i + 1)
            .flatMap(i -> IO.delay(() -> i + 2))
            .attempt()
            .unsafeRunSync();

        result.fold(
            err -> {
                err.printStackTrace();
                return Unit.unit();
            },
            a -> {
                System.out.println(a);
                return Unit.unit();
            }
        );
    }
}
