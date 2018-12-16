package io.mitallast.monad;

import io.mitallast.higher.Applicative;
import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

public class MonadApplicative<M extends Monad> implements Applicative<M> {
    private final MonadCompanion<M> companion;

    public MonadApplicative(MonadCompanion<M> companion) {
        this.companion = companion;
    }

    @Override
    public <A> Monad<M, A> pure(A a) {
        return companion.unit(a);
    }

    @Override
    public <A, B> Monad<M, B> apply(Higher<M, Function1<A, B>> fm, Higher<M, A> fa) {
        var ma = (Monad<M, A>) fa;
        var mm = (Monad<M, Function1<A, B>>) fm;
        return ma.flatMap(a -> mm.flatMap(f -> pure(f.apply(a))));
    }
}
