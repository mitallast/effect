package io.mitallast.lambda;

import io.mitallast.hlist.*;

@FunctionalInterface
public interface Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, A> {

    A apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, T10 t10, T11 t11);

    default Function10<T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, A> apply(T1 t1) {
        return (t2, t3, t4, t5, t6, t7, t8, t9, t10, t11) -> apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
    }

    default Function9<T3, T4, T5, T6, T7, T8, T9, T10, T11, A> apply(T1 t1, T2 t2) {
        return (t3, t4, t5, t6, t7, t8, t9, t10, t11) -> apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
    }

    default Function8<T4, T5, T6, T7, T8, T9, T10, T11, A> apply(T1 t1, T2 t2, T3 t3) {
        return (t4, t5, t6, t7, t8, t9, t10, t11) -> apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
    }

    default Function7<T5, T6, T7, T8, T9, T10, T11, A> apply(T1 t1, T2 t2, T3 t3, T4 t4) {
        return (t5, t6, t7, t8, t9, t10, t11) -> apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
    }

    default Function6<T6, T7, T8, T9, T10, T11, A> apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        return (t6, t7, t8, t9, t10, t11) -> apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
    }

    default Function5<T7, T8, T9, T10, T11, A> apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        return (t7, t8, t9, t10, t11) -> apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
    }

    default Function4<T8, T9, T10, T11, A> apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) {
        return (t8, t9, t10, t11) -> apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
    }

    default Function3<T9, T10, T11, A> apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) {
        return (t9, t10, t11) -> apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
    }

    default Function2<T10, T11, A> apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9) {
        return (t10, t11) -> apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
    }

    default Function1<T11, A> apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, T10 t10) {
        return (t11) -> apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
    }

    default A apply(HCons<T11, HCons<T10, HCons<T9, HCons<T8, HCons<T7, HCons<T6, HCons<T5, HCons<T4, HCons<T3, HCons<T2, HCons<T1, HNil>>>>>>>>>>> hlist11) {
        var hlist10 = hlist11.tail();
        var hlist9 = hlist10.tail();
        var hlist8 = hlist9.tail();
        var hlist7 = hlist8.tail();
        var hlist6 = hlist7.tail();
        var hlist5 = hlist6.tail();
        var hlist4 = hlist5.tail();
        var hlist3 = hlist4.tail();
        var hlist2 = hlist3.tail();
        var hlist1 = hlist2.tail();
        return apply(hlist1.head(), hlist2.head(), hlist3.head(), hlist4.head(), hlist5.head(), hlist6.head(), hlist7.head(), hlist8.head(), hlist9.head(), hlist10.head(), hlist11.head());
    }
}
