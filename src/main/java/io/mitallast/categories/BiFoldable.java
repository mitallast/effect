package io.mitallast.categories;

import io.mitallast.higher.Higher2;
import io.mitallast.kernel.Eval;
import io.mitallast.kernel.Monoid;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Function2;

public interface BiFoldable<F extends Higher2> {
    <A, B, C> C bifoldLeft(Higher2<F, A, B> fab, C c, Function2<C, A, C> f, Function2<C, B, C> g);

    <A, B, C> C bifoldRight(Higher2<F, A, B> fab, Eval<C> c,
                            Function2<A, Eval<C>, Eval<C>> f,
                            Function2<B, Eval<C>, Eval<C>> g);

    default <A, B, C> C bifoldMap(Higher2<F, A, B> fab, Function1<A, C> f, Function1<B, C> g, Monoid<C> C) {
        return bifoldLeft(fab, C.empty(), (c, a) -> C.combine(c, f.apply(a)), (c, b) -> C.combine(c, g.apply(b)));
    }
}
