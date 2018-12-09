package io.mitallast;

import io.mitallast.higher.Functor;
import io.mitallast.lambda.Function1;
import io.mitallast.maybe.Maybe;
import io.mitallast.maybe.MaybeCompanion;
import io.mitallast.monad.MonadFunctor;

public class HigherExample {
    public static void main(String... args) {
        Function1<Integer, Integer> increment = integer -> integer + 1;
        var maybe = new MaybeCompanion<Integer>();
        Maybe<Integer> one = maybe.unit(1);
        Functor<Integer, Integer, Maybe, Maybe<Integer>, Maybe<Integer>> functor = new MonadFunctor<>(maybe);
        Maybe<Integer> two = functor.map(increment, one);
        System.out.println(two);
    }
}
