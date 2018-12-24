package io.mitallast.lambda;

import io.mitallast.hlist.*;

@FunctionalInterface
public interface Function1<T1, A> {

    A apply(T1 t1);

    default A apply(HCons<T1, HNil> hlist1) {
        return apply(hlist1.head());
    }

    @SuppressWarnings("unchecked")
    default <UT1 extends T1, UA extends A> Function1<UT1, UA> cast() {
        return (Function1<UT1, UA>) this;
    }
}
