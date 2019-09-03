package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;
import io.mitallast.maybe.Maybe;

import java.util.function.Predicate;

public interface FunctorFilter<F extends Higher> {
//    Functor<F> functor();

    <A, B> Higher<F, B> mapFilter(Higher<F, A> fa, Function1<A, Maybe<B>> f);

    default <A> Higher<F, A> flattenOption(Higher<F, Maybe<A>> fa) {
        return mapFilter(fa, i -> i);
    }

    default <A> Higher<F, A> filter(Higher<F, A> fa, Predicate<A> f) {
        return mapFilter(fa, a -> {
            if (f.test(a)) return Maybe.some(a);
            else return Maybe.none();
        });
    }
}
