package io.mitallast.arrow;

import io.mitallast.higher.Higher2;
import io.mitallast.kernel.Monoid;

public interface Category<F extends Higher2> extends Compose<F> {
    <A> Higher2<F, A, A> id();

    @Override
    default <A> Monoid<Higher2<F, A, A>> algebra() {
        return new CategoryMonoid<>(this);
    }
}

final class CategoryMonoid<F extends Higher2, A> implements Monoid<Higher2<F, A, A>> {

    private final Category<F> self;

    CategoryMonoid(Category<F> self) {
        this.self = self;
    }

    @Override
    public Higher2<F, A, A> empty() {
        return self.id();
    }

    @Override
    public Higher2<F, A, A> combine(Higher2<F, A, A> f1, Higher2<F, A, A> f2) {
        return self.compose(f1, f2);
    }
}