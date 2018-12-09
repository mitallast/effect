package io.mitallast.lambda;

import io.mitallast.hlist.*;

@FunctionalInterface
public interface Function17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, A> {

    A apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, T10 t10, T11 t11, T12 t12, T13 t13, T14 t14, T15 t15, T16 t16, T17 t17);

    default A apply(HCons<T17, HCons<T16, HCons<T15, HCons<T14, HCons<T13, HCons<T12, HCons<T11, HCons<T10, HCons<T9, HCons<T8, HCons<T7, HCons<T6, HCons<T5, HCons<T4, HCons<T3, HCons<T2, HCons<T1, HNil>>>>>>>>>>>>>>>>> hlist17) {
        var hlist16 = hlist17.tail();
        var hlist15 = hlist16.tail();
        var hlist14 = hlist15.tail();
        var hlist13 = hlist14.tail();
        var hlist12 = hlist13.tail();
        var hlist11 = hlist12.tail();
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
        return apply(hlist1.head(), hlist2.head(), hlist3.head(), hlist4.head(), hlist5.head(), hlist6.head(), hlist7.head(), hlist8.head(), hlist9.head(), hlist10.head(), hlist11.head(), hlist12.head(), hlist13.head(), hlist14.head(), hlist15.head(), hlist16.head(), hlist17.head());
    }
}
