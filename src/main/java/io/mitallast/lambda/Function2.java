package io.mitallast.lambda;

import io.mitallast.hlist.*;

@FunctionalInterface
public interface Function2<T1, T2, A> {

    A apply(T1 t1, T2 t2);

    default A apply(HCons<T2, HCons<T1, HNil>> hlist2) {
        var hlist1 = hlist2.tail();
        return apply(hlist1.head(), hlist2.head());
    }
}
