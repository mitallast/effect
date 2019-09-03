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

    @SuppressWarnings("unchecked")
    default <UT1, UA> Function1<UT1, UA> castUnsafe() {
        return (Function1<UT1, UA>) this;
    }

    default <A2> Function1<T1, A2> andThen(Function1<A, A2> f) {
        return t1 -> f.apply(apply(t1));
    }
}
