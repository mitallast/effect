package io.mitallast.data;

import io.mitallast.list.List;

public final class NonEmptyList<A> {
    private final A head;
    private final List<A> tail;

    private NonEmptyList(final A head, final List<A> tail) {
        this.head = head;
        this.tail = tail;
    }

    public A head() {
        return head;
    }

    public List<A> tail() {
        return tail;
    }

    @SafeVarargs
    public static <A> NonEmptyList<A> of(A head, A... tail) {
        return new NonEmptyList<>(head, List.of(tail));
    }

    public static <A> NonEmptyList<A> of(A head, List<A> tail) {
        return new NonEmptyList<>(head, tail);
    }
}
