package io.mitallast.product;

import io.mitallast.lambda.Function2;
import io.mitallast.hlist.*;

public abstract class Product2<T1, T2> implements Product {
    private final T1 t1;
    private final T2 t2;

    protected Product2(T1 t1, T2 t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    @Override
    public Object productElement(int n) {
        switch (n) {
            case 1:
                return t1;
            case 2:
                return t2;
            default:
                throw new IndexOutOfBoundsException(n);
        }
    }

    @Override
    public int productArity() {
        return 2;
    }

    public T1 t1() {
        return t1;
    }

    public T2 t2() {
        return t2;
    }

    public Tuple2<T1, T2> toTuple() {
        return new Tuple2<>(t1, t2);
    }

    public <A> A to(Function2<T1, T2, A> f) {
        return f.apply(t1, t2);
    }

    public HCons<T2, HCons<T1, HNil>> toHList() {
        return HList.nil
            .prepend(t1)
            .prepend(t2);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('(');
        builder.append(t1);
        builder.append(',').append(t2);
        builder.append(')');
        return builder.toString();
    }

}
