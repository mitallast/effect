package io.mitallast.maybe;

import io.mitallast.categories.Applicative;
import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;
import io.mitallast.product.Tuple2;

public class MaybeApplicative implements Applicative<Maybe> {
    @Override
    public <A> Maybe<A> pure(A value) {
        return Maybe.apply(value);
    }

    @Override
    public <A, B> Higher<Maybe, B> ap(Higher<Maybe, Function1<A, B>> ff, Higher<Maybe, A> fa) {
        var ma = (Maybe<A>) fa;
        var mm = (Maybe<Function1<A, B>>) ff;
        return ma.flatMap(a -> mm.flatMap(f -> pure(f.apply(a))));
    }

    @Override
    public <A, B> Higher<Maybe, Tuple2<A, B>> product(Higher<Maybe, A> fa, Higher<Maybe, B> fb) {
        var ma = (Maybe<A>) fa;
        var mb = (Maybe<B>) fb;
        return ma.flatMap(a -> mb.flatMap(b -> pure(new Tuple2<>(a, b))));
    }
}
