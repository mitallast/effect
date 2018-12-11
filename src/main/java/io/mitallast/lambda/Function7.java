package io.mitallast.lambda;

import io.mitallast.hlist.*;

@FunctionalInterface
public interface Function7<T1, T2, T3, T4, T5, T6, T7, A> {

    A apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7);

    default Function6<T2, T3, T4, T5, T6, T7, A> apply(T1 t1) {
        return (t2, t3, t4, t5, t6, t7) -> apply(t1, t2, t3, t4, t5, t6, t7);
    }

    default Function5<T3, T4, T5, T6, T7, A> apply(T1 t1, T2 t2) {
        return (t3, t4, t5, t6, t7) -> apply(t1, t2, t3, t4, t5, t6, t7);
    }

    default Function4<T4, T5, T6, T7, A> apply(T1 t1, T2 t2, T3 t3) {
        return (t4, t5, t6, t7) -> apply(t1, t2, t3, t4, t5, t6, t7);
    }

    default Function3<T5, T6, T7, A> apply(T1 t1, T2 t2, T3 t3, T4 t4) {
        return (t5, t6, t7) -> apply(t1, t2, t3, t4, t5, t6, t7);
    }

    default Function2<T6, T7, A> apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        return (t6, t7) -> apply(t1, t2, t3, t4, t5, t6, t7);
    }

    default Function1<T7, A> apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        return (t7) -> apply(t1, t2, t3, t4, t5, t6, t7);
    }

    default A apply(HCons<T7, HCons<T6, HCons<T5, HCons<T4, HCons<T3, HCons<T2, HCons<T1, HNil>>>>>>> hlist7) {
        var hlist6 = hlist7.tail();
        var hlist5 = hlist6.tail();
        var hlist4 = hlist5.tail();
        var hlist3 = hlist4.tail();
        var hlist2 = hlist3.tail();
        var hlist1 = hlist2.tail();
        return apply(hlist1.head(), hlist2.head(), hlist3.head(), hlist4.head(), hlist5.head(), hlist6.head(), hlist7.head());
    }
}
