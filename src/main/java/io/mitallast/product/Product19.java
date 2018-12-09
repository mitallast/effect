package io.mitallast.product;

import io.mitallast.lambda.Function19;
import io.mitallast.hlist.*;

public abstract class Product19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> implements Product {
    private final T1 t1;
    private final T2 t2;
    private final T3 t3;
    private final T4 t4;
    private final T5 t5;
    private final T6 t6;
    private final T7 t7;
    private final T8 t8;
    private final T9 t9;
    private final T10 t10;
    private final T11 t11;
    private final T12 t12;
    private final T13 t13;
    private final T14 t14;
    private final T15 t15;
    private final T16 t16;
    private final T17 t17;
    private final T18 t18;
    private final T19 t19;

    protected Product19(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, T10 t10, T11 t11, T12 t12, T13 t13, T14 t14, T15 t15, T16 t16, T17 t17, T18 t18, T19 t19) {
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
        this.t4 = t4;
        this.t5 = t5;
        this.t6 = t6;
        this.t7 = t7;
        this.t8 = t8;
        this.t9 = t9;
        this.t10 = t10;
        this.t11 = t11;
        this.t12 = t12;
        this.t13 = t13;
        this.t14 = t14;
        this.t15 = t15;
        this.t16 = t16;
        this.t17 = t17;
        this.t18 = t18;
        this.t19 = t19;
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
            case 7:
                return t7;
            case 8:
                return t8;
            case 9:
                return t9;
            case 10:
                return t10;
            case 11:
                return t11;
            case 12:
                return t12;
            case 13:
                return t13;
            case 14:
                return t14;
            case 15:
                return t15;
            case 16:
                return t16;
            case 17:
                return t17;
            case 18:
                return t18;
            case 19:
                return t19;
            default:
                throw new IndexOutOfBoundsException(n);
        }
    }

    @Override
    public int productArity() {
        return 19;
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

    public T7 t7() {
        return t7;
    }

    public T8 t8() {
        return t8;
    }

    public T9 t9() {
        return t9;
    }

    public T10 t10() {
        return t10;
    }

    public T11 t11() {
        return t11;
    }

    public T12 t12() {
        return t12;
    }

    public T13 t13() {
        return t13;
    }

    public T14 t14() {
        return t14;
    }

    public T15 t15() {
        return t15;
    }

    public T16 t16() {
        return t16;
    }

    public T17 t17() {
        return t17;
    }

    public T18 t18() {
        return t18;
    }

    public T19 t19() {
        return t19;
    }

    public Tuple19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> toTuple() {
        return new Tuple19<>(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19);
    }

    public <A> A to(Function19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, A> f) {
        return f.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19);
    }

    public HCons<T19, HCons<T18, HCons<T17, HCons<T16, HCons<T15, HCons<T14, HCons<T13, HCons<T12, HCons<T11, HCons<T10, HCons<T9, HCons<T8, HCons<T7, HCons<T6, HCons<T5, HCons<T4, HCons<T3, HCons<T2, HCons<T1, HNil>>>>>>>>>>>>>>>>>>> toHList() {
        return HList.nil
            .prepend(t1)
            .prepend(t2)
            .prepend(t3)
            .prepend(t4)
            .prepend(t5)
            .prepend(t6)
            .prepend(t7)
            .prepend(t8)
            .prepend(t9)
            .prepend(t10)
            .prepend(t11)
            .prepend(t12)
            .prepend(t13)
            .prepend(t14)
            .prepend(t15)
            .prepend(t16)
            .prepend(t17)
            .prepend(t18)
            .prepend(t19);
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
        builder.append(',').append(t7);
        builder.append(',').append(t8);
        builder.append(',').append(t9);
        builder.append(',').append(t10);
        builder.append(',').append(t11);
        builder.append(',').append(t12);
        builder.append(',').append(t13);
        builder.append(',').append(t14);
        builder.append(',').append(t15);
        builder.append(',').append(t16);
        builder.append(',').append(t17);
        builder.append(',').append(t18);
        builder.append(',').append(t19);
        builder.append(')');
        return builder.toString();
    }

}
