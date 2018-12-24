package io.mitallast.maybe;

import io.mitallast.categories.Applicative;
import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;
import io.mitallast.product.Tuple2;

public final class MaybeApplicative implements Applicative<Maybe> {
    @Override
    public <A> Maybe<A> pure(A value) {
        return Maybe.apply(value);
    }

    @Override
    public <A, B> Maybe<B> ap(Higher<Maybe, Function1<A, B>> ff, Higher<Maybe, A> fa) {
        return $(fa).flatMap(a -> $(ff).flatMap(f -> pure(f.apply(a))));
    }

    @Override
    public <A, B> Maybe<Tuple2<A, B>> product(Higher<Maybe, A> fa, Higher<Maybe, B> fb) {
        return $(fa).flatMap(a -> $(fb).flatMap(b -> pure(new Tuple2<>(a, b))));
    }

    private <A> Maybe<A> $(Higher<Maybe, A> higher) {
        return (Maybe<A>) higher;
    }
}
