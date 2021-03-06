package io.mitallast.lambda;

import io.mitallast.hlist.*;

@FunctionalInterface
public interface Function2<T1, T2, A> {

    A apply(T1 t1, T2 t2);

    default Function1<T2, A> apply(T1 t1) {
        return (t2) -> apply(t1, t2);
    }

    default A apply(HCons<T2, HCons<T1, HNil>> hlist2) {
        var hlist1 = hlist2.tail();
        return apply(hlist1.head(), hlist2.head());
    }

    @SuppressWarnings("unchecked")
    default <UT1 extends T1, UT2 extends T2, UA extends A> Function2<UT1, UT2, UA> cast() {
        return (Function2<UT1, UT2, UA>) this;
    }
}
