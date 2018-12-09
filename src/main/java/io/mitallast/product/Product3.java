package io.mitallast.product;

import io.mitallast.lambda.Function3;
import io.mitallast.hlist.*;

public abstract class Product3<T1, T2, T3> implements Product {
    private final T1 t1;
    private final T2 t2;
    private final T3 t3;

    protected Product3(T1 t1, T2 t2, T3 t3) {
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
    }

    @Override
    public Object productElement(int n) {
        switch (n) {
            case 1:
                return t1;
            case 2:
                return t2;
            case 3:
                return t3;
            default:
                throw new IndexOutOfBoundsException(n);
        }
    }

    @Override
    public int productArity() {
        return 3;
    }

    public T1 t1() {
        return t1;
    }

    public T2 t2() {
        return t2;
    }

    public T3 t3() {
        return t3;
    }

    public Tuple3<T1, T2, T3> toTuple() {
        return new Tuple3<>(t1, t2, t3);
    }

    public <A> A to(Function3<T1, T2, T3, A> f) {
        return f.apply(t1, t2, t3);
    }

    public HCons<T3, HCons<T2, HCons<T1, HNil>>> toHList() {
        return HList.nil
            .prepend(t1)
            .prepend(t2)
            .prepend(t3);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('(');
        builder.append(t1);
        builder.append(',').append(t2);
        builder.append(',').append(t3);
        builder.append(')');
        return builder.toString();
    }

}
