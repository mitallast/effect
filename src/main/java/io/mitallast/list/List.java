package io.mitallast.list;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Function2;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

public abstract class List<A> implements Iterable<A>, Higher<List, A> {
    private List() {
    }

    public abstract int size();

    public abstract boolean isEmpty();

    public final boolean nonEmpty() {
        return !isEmpty();
    }

    public abstract A head();

    public A last() {
        if (isEmpty()) throw new NoSuchElementException();
        var curr = this;
        while (curr.tail().nonEmpty()) {
            curr = curr.tail();
        }
        return curr.head();
    }

    public abstract List<A> tail();

    public final List<A> prepend(A value) {
        return new Cons<>(value, this);
    }

    public final List<A> prepend(final List<A> all) {
        List<A> next = all;
        List<A> current = this;
        do {
            if (next.isEmpty()) {
                return current;
            } else {
                current = current.prepend(next.head());
                next = next.tail();
            }
        } while (true);
    }

    public final List<A> prepend(final Iterable<A> all) {
        List<A> acc = this;
        for (A a : all) {
            acc = acc.prepend(a);
        }
        return acc;
    }

    public <B> List<B> map(Function1<A, B> f) {
        List<A> current = this;
        List<B> build = nil();
        do {
            if (current.isEmpty()) {
                return build;
            } else {
                build = build.prepend(f.apply(current.head()));
                current = current.tail();
            }

        } while (true);
    }

    public <B> List<B> flatMap(Function1<A, List<B>> f) {
        List<A> current = this;
        List<B> build = nil();
        do {
            if (current.isEmpty()) {
                return build;
            } else {
                var flat = f.apply(current.head());
                build = build.prepend(flat);
                current = current.tail();
            }

        } while (true);
    }

    public <B> B foldLeft(final B b, final Function2<B, A, B> f) {
        List<A> current = this;
        B acc = b;
        do {
            if (current.isEmpty()) {
                return acc;
            } else {
                acc = f.apply(acc, current.head());
                current = current.tail();
            }
        } while (true);
    }

    public List<A> reverse() {
        List<A> current = this;
        List<A> build = nil();
        do {
            if (current.isEmpty()) {
                return build;
            } else {
                build = build.prepend(current.head());
                current = current.tail();
            }

        } while (true);
    }

    public List<A> drop(final int n) {
        var curr = this;
        var count = n;
        while (curr.nonEmpty() && count > 0) {
            curr = curr.tail();
            count--;
        }
        return curr;
    }

    public A apply(final int n) {
        var rest = drop(n);
        if (n < 0 || rest.isEmpty()) throw new IndexOutOfBoundsException();
        return rest.head();
    }

    public boolean forall(Predicate<A> p) {
        var curr = this;
        while (curr.nonEmpty()) {
            if (!p.test(curr.head())) return false;
            curr = curr.tail();
        }
        return true;
    }

    public boolean exists(Predicate<A> p) {
        var curr = this;
        while (curr.nonEmpty()) {
            if (p.test(curr.head())) return true;
            curr = curr.tail();
        }
        return false;
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

        @Override
        public Iterator<A> iterator() {
            return new ListIterator<>(this);
        }
    }

    private static final class Nil<A> extends List<A> {
        private final static Nil instance = new Nil();

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

        @Override
        public Iterator<A> iterator() {
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public A next() {
                    throw new NoSuchElementException();
                }
            };
        }
    }

    private static final class ListIterator<A> implements Iterator<A> {
        private List<A> head;

        private ListIterator(List<A> head) {
            this.head = head;
        }

        @Override
        public boolean hasNext() {
            return head.nonEmpty();
        }

        @Override
        public A next() {
            if (head.isEmpty()) {
                throw new NoSuchElementException();
            } else {
                var value = head.head();
                head = head.tail();
                return value;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <A> List<A> nil() {
        return (List<A>) Nil.instance;
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

    public static <A> List<A> fill(int size, A value) {
        List<A> list = nil();
        for (int i = 0; i < size; i++) {
            list = list.prepend(value);
        }
        return list;
    }
}
