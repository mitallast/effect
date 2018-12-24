package io.mitallast;

import io.mitallast.categories.Applicative;
import io.mitallast.either.EitherApplicative;
import io.mitallast.higher.Higher;
import io.mitallast.io.IO;
import io.mitallast.maybe.MaybeApplicative;

public class HigherExample {
    private static <F extends Higher> Higher<F, Integer> program(Applicative<F> $) {
        var p = $.pure(1);
        var mapped = $.map(p, x -> x + 2);
        return $.ap($.pure(x -> x + 3), mapped);
    }

    private static void maybeApply() {
        var applicative = new MaybeApplicative();
        var x = program(applicative);
        System.out.println("maybe:");
        System.out.println(x);
    }

    private static void eitherApply() {
        var applicative = new EitherApplicative<String>();
        var x = program(applicative);
        System.out.println("either:");
        System.out.println(x);
    }

    private static void ioApply() {
        var applicative = IO.effect();
        var x = (IO<Integer>) program(applicative);
        System.out.println("io:");
        System.out.println(x.unsafeRunSync());
    }

    public static void main(String... args) {
        maybeApply();
        eitherApply();
        ioApply();
    }
}
