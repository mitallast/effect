package io.mitallast.monad;

import io.mitallast.higher.Functor;
import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

public class MonadFunctor<
    A,
    B,
    F extends Monad & Higher,
    FA extends Monad<A> & Higher<F, A>,
    FB extends Monad<B> & Higher<F, B>
    > implements Functor<A, B, F, FA, FB> {

    private final MonadCompanion<B, F, FB> companion;

    public MonadFunctor(MonadCompanion<B, F, FB> companion) {
        this.companion = companion;
    }

    @Override
    public FB map(Function1<A, B> fn, FA ht) {
        return ht.flatMap(a -> companion.unit(fn.apply(a)));
    }
}
