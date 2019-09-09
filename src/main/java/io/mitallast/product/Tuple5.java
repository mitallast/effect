package io.mitallast.product;

public final class Tuple5<T1, T2, T3, T4, T5> extends Product5<T1, T2, T3, T4, T5> implements Tuple {
    protected Tuple5(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        super(t1, t2, t3, t4, t5);
    }
}

