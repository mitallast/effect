package io.mitallast.categories;

import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

public interface FlatMap<F extends Higher> extends Apply<F> {
    <A, B> Higher<F, B> flatMap(Higher<F, A> fa, Function1<A, Higher<F, B>> f);

    default <A> Higher<F, A> flatten(Higher<F, Higher<F, A>> ffa) {
        return flatMap(ffa, fa -> fa);
    }

    @Override
    default <A, B> Higher<F, B> ap(Higher<F, Function1<A, B>> ff, Higher<F, A> fa) {
        return flatMap(ff, f -> map(fa, f));
    }

    /**
     * Keeps calling `f` until a `scala.util.Right[B]` is returned.
     * <p>
     * Based on Phil Freeman's
     * [[http://functorial.com/stack-safety-for-free/index.pdf Stack Safety for Free]].
     * <p>
     * Implementations of this method should use constant stack space relative to `f`.
     */
    // def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B]
    <A, B> Higher<F, B> tailRecM(A a, Function1<A, Higher<F, Either<A, B>>> f);
}
