package io.mitallast;

import io.mitallast.io.IO;
import io.mitallast.maybe.Maybe;
import io.mitallast.stream.Stream;

public class StreamExample {
    public static void main(String... args) {
        var F = IO.effect();
        var compiler = Stream.Compiler.sync(F);
        var task = (IO<Maybe<String>>) Stream.eval(IO.delay(() -> "Hello world"))
            .compile(compiler)
            .last();

        var output = task.unsafeRunSync();
        System.out.println(output);


        for (int i = 0; i < 100; i++) {
            int max = 10000000;
            long start = System.currentTimeMillis();
            var task2 = (IO<Maybe<Integer>>) Stream.<IO>range(0, max)
                .compile(compiler)
                .last();
            task2.unsafeRunSync();
            long end = System.currentTimeMillis();
            System.out.println(String.format("run %d at %d ms", max, end - start));
            System.out.println(String.format("%f tps", max * 1000.0 / (end - start)));
        }
    }
}
