package io.mitallast.hlist;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class HCons<H, T extends HList> implements HList {
    private final H head;
    private final T tail;

    HCons(H head, T tail) {
        this.head = head;
        this.tail = tail;
    }

    public H head() {
        return head;
    }

    public T tail() {
        return tail;
    }

    public <HH> HCons<HH, HCons<H, T>> prepend(HH hh) {
        return new HCons<>(hh, this) {
        };
    }

    public void examine() {
        var type = ((ParameterizedType) getClass().getGenericSuperclass());
        Type[] actualTypeArguments = type.getActualTypeArguments();
        for (Type actualTypeArgument : actualTypeArguments) {
            System.out.println(actualTypeArgument);
        }

    }

    @Override
    public String toString() {
        return head + " :: " + tail;
    }
}
