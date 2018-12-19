package io.mitallast.categories;

import io.mitallast.higher.Higher2;
import io.mitallast.kernel.Monoid;
import io.mitallast.lambda.Function1;

import java.util.function.BiFunction;

public interface BiFoldable<F extends Higher2> {
    <A, B, C> C bifoldLeft(Higher2<F, A, B> fab, C c, BiFunction<C, A, C> f, BiFunction<C, B, C> g);

    <A, B, C> C bifoldRight(Higher2<F, A, B> fab, C c, BiFunction<A, C, C> f, BiFunction<B, C, C> g);

    default <A, B, C> C bifoldMap(Higher2<F, A, B> fab, Function1<A, C> f, Function1<B, C> g, Monoid<C> C) {
        return bifoldLeft(fab, C.empty(), (c, a) -> C.combine(c, f.apply(a)), (c, b) -> C.combine(c, g.apply(b)));
    }
}
