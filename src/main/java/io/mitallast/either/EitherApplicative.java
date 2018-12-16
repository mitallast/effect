package io.mitallast.either;

import io.mitallast.higher.Applicative;
import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

public class EitherApplicative<L> implements Applicative<Either<L, ?>> {

    @Override
    public <R> Either<L, R> pure(R a) {
        return new Right<>(a);
    }

    @Override
    public <A, B> Either<L, B> apply(Higher<Either<L, ?>, Function1<A, B>> fm, Higher<Either<L, ?>, A> fa) {
        var ea = (Either<L, A>) fa;
        var em = (Either<L, Function1<A, B>>) fm;
        return ea.flatMap(a -> em.map(m -> m.apply(a)));
    }
}
