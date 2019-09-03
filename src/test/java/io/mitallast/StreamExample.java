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
    }
}
