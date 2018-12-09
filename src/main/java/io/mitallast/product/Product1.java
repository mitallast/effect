package io.mitallast.product;

import io.mitallast.lambda.Function1;
import io.mitallast.hlist.*;

public abstract class Product1<T1> implements Product {
    private final T1 t1;

    protected Product1(T1 t1) {
        this.t1 = t1;
    }

    @Override
    public Object productElement(int n) {
        switch (n) {
            case 1:
                return t1;
            default:
                throw new IndexOutOfBoundsException(n);
        }
    }

    @Override
    public int productArity() {
        return 1;
    }

    public T1 t1() {
        return t1;
    }

    public Tuple1<T1> toTuple() {
        return new Tuple1<>(t1);
    }

    public <A> A to(Function1<T1, A> f) {
        return f.apply(t1);
    }

    public HCons<T1, HNil> toHList() {
        return HList.nil
            .prepend(t1);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('(');
        builder.append(t1);
        builder.append(')');
        return builder.toString();
    }

}
