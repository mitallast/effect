package io.mitallast.lambda;

import io.mitallast.hlist.*;

@FunctionalInterface
public interface Function5<T1, T2, T3, T4, T5, A> {

    A apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5);

    default Function4<T2, T3, T4, T5, A> apply(T1 t1) {
        return (t2, t3, t4, t5) -> apply(t1, t2, t3, t4, t5);
    }

    default Function3<T3, T4, T5, A> apply(T1 t1, T2 t2) {
        return (t3, t4, t5) -> apply(t1, t2, t3, t4, t5);
    }

    default Function2<T4, T5, A> apply(T1 t1, T2 t2, T3 t3) {
        return (t4, t5) -> apply(t1, t2, t3, t4, t5);
    }

    default Function1<T5, A> apply(T1 t1, T2 t2, T3 t3, T4 t4) {
        return (t5) -> apply(t1, t2, t3, t4, t5);
    }

    default A apply(HCons<T5, HCons<T4, HCons<T3, HCons<T2, HCons<T1, HNil>>>>> hlist5) {
        var hlist4 = hlist5.tail();
        var hlist3 = hlist4.tail();
        var hlist2 = hlist3.tail();
        var hlist1 = hlist2.tail();
        return apply(hlist1.head(), hlist2.head(), hlist3.head(), hlist4.head(), hlist5.head());
    }
}
