package io.mitallast.list;

import io.mitallast.higher.Higher;

public abstract class List<A> implements Higher<List, A> {
    private List() {
    }

    public abstract int size();

    public abstract boolean isEmpty();

    public final boolean nonEmpty() {
        return !isEmpty();
    }

    public abstract A head();

    public abstract List<A> tail();

    public List<A> prepend(A value) {
        return new Cons<>(value, this);
    }

    private static final class Cons<A> extends List<A> {
        private final A head;
        private final List<A> tail;

        private Cons(A head, List<A> tail) {
            this.head = head;
            this.tail = tail;
        }

        @Override
        public int size() {
            int size = 1;
            List<A> list = tail;
            while (!list.isEmpty()) {
                list = list.tail();
                size++;
            }
            return size;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public A head() {
            return head;
        }

        @Override
        public List<A> tail() {
            return tail;
        }
    }

    private static final class Nil<A> extends List<A> {
        private Nil() {
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public A head() {
            throw new ArrayIndexOutOfBoundsException();
        }

        @Override
        public List<A> tail() {
            return this;
        }
    }

    public static <A> List<A> nil() {
        return new Nil<>();
    }

    public static <A> List<A> empty() {
        return nil();
    }

    public static <A> List<A> of(A value) {
        return new Cons<>(value, nil());
    }

    public static <A> List<A> of(A... values) {
        List<A> list = nil();
        for (A value : values) {
            list = new Cons<>(value, list);
        }
        return list;
    }
}
