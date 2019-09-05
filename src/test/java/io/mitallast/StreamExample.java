package io.mitallast;

import io.mitallast.io.IO;
import io.mitallast.kernel.Unit;
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


        for (int i = 0; i < Integer.MAX_VALUE; i++) {
//            int max = Integer.MAX_VALUE;
            int max = 10000000;
            long start = System.currentTimeMillis();
            var task2 = (IO<Unit>) Stream.<IO>range(0, max)
                .compile(compiler)
                .drain();
            task2.unsafeRunSync();
            long end = System.currentTimeMillis();
            System.out.println(String.format("run %d at %d ms", max, end - start));
            System.out.println(String.format("%f tps", max * 1000.0 / (end - start)));
        }
    }
}
