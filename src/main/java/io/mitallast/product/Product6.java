package io.mitallast.product;

import io.mitallast.lambda.Function6;
import io.mitallast.hlist.*;

public abstract class Product6<T1, T2, T3, T4, T5, T6> implements Product {
    private final T1 t1;
    private final T2 t2;
    private final T3 t3;
    private final T4 t4;
    private final T5 t5;
    private final T6 t6;

    protected Product6(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
        this.t4 = t4;
        this.t5 = t5;
        this.t6 = t6;
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
            case 4:
                return t4;
            case 5:
                return t5;
            case 6:
                return t6;
            default:
                throw new IndexOutOfBoundsException(n);
        }
    }

    @Override
    public int productArity() {
        return 6;
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

    public T4 t4() {
        return t4;
    }

    public T5 t5() {
        return t5;
    }

    public T6 t6() {
        return t6;
    }

    public Tuple6<T1, T2, T3, T4, T5, T6> toTuple() {
        return new Tuple6<>(t1, t2, t3, t4, t5, t6);
    }

    public <A> A to(Function6<T1, T2, T3, T4, T5, T6, A> f) {
        return f.apply(t1, t2, t3, t4, t5, t6);
    }

    public HCons<T6, HCons<T5, HCons<T4, HCons<T3, HCons<T2, HCons<T1, HNil>>>>>> toHList() {
        return HList.nil
            .prepend(t1)
            .prepend(t2)
            .prepend(t3)
            .prepend(t4)
            .prepend(t5)
            .prepend(t6);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('(');
        builder.append(t1);
        builder.append(',').append(t2);
        builder.append(',').append(t3);
        builder.append(',').append(t4);
        builder.append(',').append(t5);
        builder.append(',').append(t6);
        builder.append(')');
        return builder.toString();
    }

}
