package io.mitallast.categories;

import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

/**
 * An applicative that also allows you to raise and or handle an error value.
 * <p>
 * This type class allows one to abstract over error-handling applicatives.
 */
public interface ApplicativeError<F extends Higher, E> extends Applicative<F> {
    <A> Higher<F, A> raiseError(E e);

    <A> Higher<F, A> handleErrorWith(Higher<F, A> fa, Function1<E, Higher<F, A>> f);

    default <A> Higher<F, A> handleError(Higher<F, A> fa, Function1<E, A> f) {
        return handleErrorWith(fa, e -> pure(f.apply(e)));
    }

    default <A> Higher<F, Either<E, A>> attempt(Higher<F, A> fa) {
        return handleErrorWith(map(fa, Either::right), e -> pure(Either.left(e)));
    }
}
