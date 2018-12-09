package io.mitallast.lambda;

import io.mitallast.hlist.*;

@FunctionalInterface
public interface Function3<T1, T2, T3, A> {

    A apply(T1 t1, T2 t2, T3 t3);

    default A apply(HCons<T3, HCons<T2, HCons<T1, HNil>>> hlist3) {
        var hlist2 = hlist3.tail();
        var hlist1 = hlist2.tail();
        return apply(hlist1.head(), hlist2.head(), hlist3.head());
    }
}
