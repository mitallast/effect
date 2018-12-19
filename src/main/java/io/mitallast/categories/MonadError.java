package io.mitallast.categories;

import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

import java.util.function.Predicate;
import java.util.function.Supplier;

public interface MonadError<F extends Higher, E> extends ApplicativeError<F, E>, Monad<F> {
    default <A> Higher<F, A> ensure(Higher<F, A> fa, Supplier<E> error, Predicate<A> predicate) {
        return flatMap(fa, a -> {
            if (predicate.test(a)) {
                return pure(a);
            } else {
                return raiseError(error.get());
            }
        });
    }

    default <A> Higher<F, A> ensureOr(Higher<F, A> fa, Function1<A, E> error, Predicate<A> predicate) {
        return flatMap(fa, a -> {
            if (predicate.test(a)) {
                return pure(a);
            } else {
                return raiseError(error.apply(a));
            }
        });
    }

    default <A> Higher<F, A> rethrow(Higher<F, Either<E, A>> fa) {
        return flatMap(fa, e -> e.fold(ee -> raiseError(ee), a -> pure(a)));
    }
}
