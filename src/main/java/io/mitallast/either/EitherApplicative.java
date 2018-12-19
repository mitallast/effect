package io.mitallast.either;


import io.mitallast.categories.Applicative;
import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;
import io.mitallast.product.Tuple2;

public class EitherApplicative<L> implements Applicative<Either<L, ?>> {
    @Override
    public <A> Either<L, A> pure(A x) {
        return Either.right(x);
    }

    @Override
    public <A, B> Higher<Either<L, ?>, B> ap(Higher<Either<L, ?>, Function1<A, B>> ff, Higher<Either<L, ?>, A> fa) {
        var ea = (Either<L, A>) fa;
        var em = (Either<L, Function1<A, B>>) ff;
        return ea.flatMap(a -> em.map(m -> m.apply(a)));
    }

    @Override
    public <A, B> Higher<Either<L, ?>, Tuple2<A, B>> product(Higher<Either<L, ?>, A> fa, Higher<Either<L, ?>, B> fb) {
        var ea = (Either<L, A>) fa;
        var eb = (Either<L, B>) fb;
        return ea.flatMap(a -> eb.flatMap(b -> pure(new Tuple2<>(a, b))));
    }
}
