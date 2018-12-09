package io.mitallast.lambda;

import io.mitallast.hlist.*;

@FunctionalInterface
public interface Function4<T1, T2, T3, T4, A> {

    A apply(T1 t1, T2 t2, T3 t3, T4 t4);

    default A apply(HCons<T4, HCons<T3, HCons<T2, HCons<T1, HNil>>>> hlist4) {
        var hlist3 = hlist4.tail();
        var hlist2 = hlist3.tail();
        var hlist1 = hlist2.tail();
        return apply(hlist1.head(), hlist2.head(), hlist3.head(), hlist4.head());
    }
}
