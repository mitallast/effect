package io.mitallast.arrow;

import io.mitallast.higher.Higher2;
import io.mitallast.kernel.Semigroup;

public interface Compose<F extends Higher2> {

    <A, B, C> Higher2<F, A, C> compose(Higher2<F, B, C> f,
                                       Higher2<F, A, B> g);

    <A, B, C> Higher2<F, A, C> andThen(Higher2<F, A, B> f,
                                       Higher2<F, B, C> g);

    default <A> Semigroup<Higher2<F, A, A>> algebra() {
        return new ComposeSemigroup<>(this);
    }
}

final class ComposeSemigroup<F extends Higher2, A> implements Semigroup<Higher2<F, A, A>> {
    private final Compose<F> self;

    ComposeSemigroup(Compose<F> self) {
        this.self = self;
    }

    @Override
    public Higher2<F, A, A> combine(Higher2<F, A, A> f1, Higher2<F, A, A> f2) {
        return self.compose(f1, f2);
    }
}
