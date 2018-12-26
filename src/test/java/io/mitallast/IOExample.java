package io.mitallast;

import io.mitallast.concurrent.ExecutionContext;
import io.mitallast.io.IO;
import io.mitallast.kernel.Unit;

import java.time.Duration;

public class IOExample {
    public static void main(String... args) {
        var ec = ExecutionContext.global;
        var cs = IO.contextShift(ec);
        var timer = IO.timer(ec);

        var result = IO.pure(123)
            .flatMap(i -> IO.shift(cs).map(u -> i))
            .map(i -> i + 1)
            .flatMap(i -> IO.delay(() -> i + 2))
            .attempt()
            .unsafeRunSync();

        result.foreach(Throwable::printStackTrace, System.out::println);

        var s1 = IO.sleep(Duration.ofMillis(100), timer);
        var s2 = IO.sleep(Duration.ofMillis(200), timer);
        IO.race(s1, s2, cs).unsafeRunSync().foreach(
            u1 -> System.out.println("u1 finished"),
            u2 -> System.out.println("u2 finished")
        );

        System.out.println("start");
        IO.sleep(Duration.ofMillis(1000), timer)
            .start(cs)
            .flatMap(f1 -> {
                System.out.println("started f1, join");
                return ((IO<Unit>) f1.join());
            })
            .flatMap(u1 -> {
                System.out.println("joined, complete");
                return IO.unit();
            })
            .unsafeRunSync();
        System.out.println("finish");
    }
}
