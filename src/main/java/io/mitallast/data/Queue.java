package io.mitallast.data;

import io.mitallast.lambda.Function2;
import io.mitallast.list.List;
import io.mitallast.product.Tuple;
import io.mitallast.product.Tuple2;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

public final class Queue<A> implements Iterable<A> {
    private final List<A> in;
    private final List<A> out;

    protected Queue(final List<A> in, final List<A> out) {
        this.in = in;
        this.out = out;
    }

    public A apply(int n) {
        var olen = out.size();
        if (n < olen) return out.apply(n);
        else {
            var m = n - olen;
            var ilen = in.size();
            if (m < ilen) return in.apply(ilen - m - 1);
            else throw new NoSuchElementException("index out of range");
        }
    }

    @Override
    public Iterator<A> iterator() {
        return new Iterator<A>() {
            final Iterator<A> i = in.iterator();
            final Iterator<A> o = out.iterator();

            @Override
            public boolean hasNext() {
                return i.hasNext() || o.hasNext();
            }

            @Override
            public A next() {
                if (i.hasNext()) {
                    return i.next();
                } else return o.next();
            }
        };
    }

    public boolean isEmpty() {
        return in.isEmpty() && out.isEmpty();
    }

    public A head() {
        if (out.nonEmpty()) return out.head();
        else if (in.nonEmpty()) return in.last();
        else throw new NoSuchElementException();
    }

    public Queue<A> tail() {
        if (out.nonEmpty()) return new Queue<>(in, out.tail());
        else if (in.nonEmpty()) return new Queue<>(List.nil(), in.reverse().tail());
        else throw new NoSuchElementException();
    }

    public boolean forall(Predicate<A> p) {
        return in.forall(p) && out.forall(p);
    }

    public boolean exists(Predicate<A> p) {
        return in.exists(p) || out.exists(p);
    }

    public int size() {
        return in.size() + out.size();
    }

    public <B> B foldLeft(B init, Function2<B, A, B> f) {
        var result = init;
        for (A a : this) {
            result = f.apply(result, a);
        }
        return result;
    }

    public Queue<A> append(A elem) {
        return new Queue<>(in, out.prepend(elem));
    }

    public Queue<A> prepend(A elem) {
        return enqueue(elem);
    }

    public Queue<A> enqueue(A elem) {
        return new Queue<>(in.prepend(elem), out);
    }

    public Queue<A> enqueue(Iterable<A> elem) {
        return new Queue<>(in.prepend(elem), out);
    }

    public Tuple2<A, Queue<A>> dequeue() {
        if (out.isEmpty() && in.nonEmpty()) {
            var rev = in.reverse();
            return Tuple.of(rev.head(), new Queue<>(List.nil(), rev.tail()));
        } else if (out.nonEmpty()) {
            return Tuple.of(out.head(), new Queue<>(in, out.tail()));
        } else throw new NoSuchElementException();
    }

    // static

    private final static Queue empty = new Queue<>(List.nil(), List.nil());

    @SuppressWarnings("unchecked")
    public static <A> Queue<A> empty() {
        return (Queue<A>) empty;
    }

    @SafeVarargs
    public static <A> Queue<A> apply(A... xs) {
        return new Queue<>(List.nil(), List.of(xs));
    }
}
