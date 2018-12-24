package io.mitallast.categories;

import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

public interface StackSafeMonad<F extends Higher> extends Monad<F> {

    @Override
    default <A, B> Higher<F, B> tailRecM(A a, Function1<A, Higher<F, Either<A, B>>> f) {
        return flatMap(f.apply(a), e -> e.fold(
            al -> tailRecM(al, f),
            b -> pure(b)
        ));
    }
}
