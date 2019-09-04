package io.mitallast.stream;

import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Function2;
import io.mitallast.list.List;
import io.mitallast.maybe.Maybe;
import io.mitallast.product.Tuple2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

abstract class Chunk<O> implements Iterable<O> {

    private Chunk() {
    }

    abstract public int size();

    abstract public O apply(int i);

    abstract public void copyToArray(O[] xs, int start);

    abstract public void copyToBuffer(ArrayList<O> buffer);

    final public Chunk<O> drop(int n) {
        return splitAt(n).t2();
    }

    final public Chunk<O> filter(Predicate<O> p) {
        if (isEmpty()) {
            return Chunk.empty();
        } else {
            var buffer = new ArrayList<O>(size());
            int s = size();
            for (int i = 0; i < s; i++) {
                var o = apply(i);
                if (p.test(o)) {
                    buffer.add(o);
                }
            }
            return new IndexedSeqChunk<>(buffer);
        }
    }

    final public Maybe<O> find(Predicate<O> p) {
        int s = size();
        for (int i = 0; i < s; i++) {
            var o = apply(i);
            if (p.test(o)) {
                return Maybe.some(o);
            }
        }
        return Maybe.none();
    }

    public <O2> Chunk<O2> flatMap(Function1<O, Chunk<O2>> f) {
        if (isEmpty()) return Chunk.empty();
        else {
            int s = size();
            var buf = new ArrayList<Chunk<O2>>(s);
            var totalSize = 0;
            for (int i = 0; i < s; i++) {
                var o = apply(i);
                var m = f.apply(o);
                buf.add(m);
                totalSize += m.size();
            }
            var b = new ArrayList<O2>(totalSize);
            for (Chunk<O2> c : buf) {
                c.copyToBuffer(b);
            }
            return new IndexedSeqChunk<>(b);
        }
    }

    final public <A> A foldLeft(A init, Function2<A, O, A> f) {
        var acc = init;
        int s = size();
        for (int i = 0; i < s; i++) {
            acc = f.apply(acc, apply(i));
        }
        return acc;
    }

    final public boolean forall(Predicate<O> p) {
        int s = size();
        for (int i = 0; i < s; i++) {
            if (!p.test(apply(i))) return false;
        }
        return true;
    }

    final public void foreach(Consumer<O> f) {
        int s = size();
        for (int i = 0; i < s; i++) {
            f.accept(apply(i));
        }
    }

    final public Maybe<O> head(Consumer<O> f) {
        if (isEmpty()) return Maybe.none();
        else return Maybe.some(apply(0));
    }

    final public boolean isEmpty() {
        return size() == 0;
    }

    final public boolean nonEmpty() {
        return !isEmpty();
    }

    final public Iterator<O> iterator() {
        return new Iterator<>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < size();
            }

            @Override
            public O next() {
                var result = apply(i);
                i++;
                return result;
            }
        };
    }

    final public Maybe<Integer> indexWhere(Predicate<O> p) {
        int s = size();
        for (int i = 0; i < s; i++) {
            if (p.test(apply(i))) {
                return Maybe.some(i);
            }
        }
        return Maybe.none();
    }

    final public Maybe<O> last() {
        if (isEmpty()) return Maybe.none();
        else return Maybe.some(apply(size() - 1));
    }

    public <O2> Chunk<O2> map(Function1<O, O2> f) {
        var s = size();
        var b = new ArrayList<O2>(s);
        for (int i = 0; i < s; i++) {
            b.add(f.apply(apply(i)));
        }
        return new IndexedSeqChunk<>(b);
    }

    final public <S, O2> Tuple2<S, Chunk<O2>> mapAccumulate(S init, Function2<S, O, Tuple2<S, O2>> f) {
        var s = size();
        var b = new ArrayList<O2>(s);
        var acc = init;
        for (int i = 0; i < s; i++) {
            var t = f.apply(acc, apply(i));
            b.add(t.t2());
            acc = t.t1();
        }
        return new Tuple2<>(acc, new IndexedSeqChunk<>(b));
    }

    final public Iterator<O> reverseIterator() {
        return new Iterator<>() {
            int i = size() - 1;

            @Override
            public boolean hasNext() {
                return i >= 0;
            }

            @Override
            public O next() {
                var result = apply(i);
                i--;
                return result;
            }
        };
    }

    public Tuple2<Chunk<O>, Chunk<O>> splitAt(int n) {
        if (n <= 0) return new Tuple2<>(Chunk.empty(), this);
        else if (n >= size()) return new Tuple2<>(this, Chunk.empty());
        else return splitAtChunk_(n);
    }

    protected abstract Tuple2<Chunk<O>, Chunk<O>> splitAtChunk_(int n);


    // static

    @SuppressWarnings("unchecked")
    public static <O> Chunk<O> empty() {
        return (Chunk<O>) EmptyChunk.instance;
    }

    private final static class EmptyChunk extends Chunk<Object> {
        private static final EmptyChunk instance = new EmptyChunk();

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Object apply(final int i) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public void copyToArray(final Object[] xs, final int start) {
        }

        @Override
        public void copyToBuffer(final ArrayList<Object> buffer) {
        }

        @Override
        protected Tuple2<Chunk<Object>, Chunk<Object>> splitAtChunk_(final int n) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public <O2> Chunk<O2> map(final Function1<Object, O2> f) {
            return empty();
        }
    }

    public static <O> Chunk<O> singleton(O value) {
        return new Singleton<>(value);
    }

    private final static class Singleton<O> extends Chunk<O> {
        private final O value;

        private Singleton(final O value) {
            this.value = value;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public O apply(final int i) {
            if (i == 0) return value;
            else throw new IndexOutOfBoundsException();
        }

        @Override
        public void copyToArray(final O[] xs, final int start) {
            xs[start] = value;
        }

        @Override
        public void copyToBuffer(final ArrayList<O> buffer) {
            buffer.add(value);
        }

        @Override
        protected Tuple2<Chunk<O>, Chunk<O>> splitAtChunk_(final int n) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public <O2> Chunk<O2> map(final Function1<O, O2> f) {
            return Chunk.singleton(f.apply(value));
        }
    }

    public static <O> Chunk<O> indexedSeq(ArrayList<O> seq) {
        if (seq.isEmpty()) {
            return empty();
        } else if (seq.size() == 1) {
            return singleton(seq.get(0));
        } else {
            return new IndexedSeqChunk<>(seq);
        }
    }

    public static <O> Chunk<O> buffer(ArrayList<O> seq) {
        return indexedSeq(seq);
    }

    public static <A> Chunk<A> concat(List<Chunk<A>> chunks) {
        var size = chunks.foldLeft(0, (acc, c) -> acc + c.size());
        var builder = new ArrayList<A>(size);
        for (Chunk<A> chunk : chunks) {
            chunk.copyToBuffer(builder);
        }
        return Chunk.buffer(builder);
    }

    public static <A> Chunk<A> seq(Collection<A> seq) {
        return indexedSeq(new ArrayList<>(seq));
    }

    public static <A> Chunk<A> seq(List<A> seq) {
        var buffer = new ArrayList<A>(seq.size());
        for (A a : seq) {
            buffer.add(a);
        }
        return indexedSeq(buffer);
    }

    public static <A> Chunk<A> fill(int size, A value) {
        var buffer = new ArrayList<A>(size);
        for (int i = 0; i < size; i++) {
            buffer.add(value);
        }
        return indexedSeq(buffer);
    }

    private final static class IndexedSeqChunk<O> extends Chunk<O> {
        private final ArrayList<O> seq;

        private IndexedSeqChunk(final ArrayList<O> seq) {
            this.seq = seq;
        }

        @Override
        public int size() {
            return seq.size();
        }

        @Override
        public O apply(final int i) {
            return seq.get(i);
        }

        @Override
        public void copyToArray(final O[] xs, final int start) {
            var array = seq.toArray();
            System.arraycopy(array, 0, xs, start, xs.length - start);
        }

        @Override
        public void copyToBuffer(final ArrayList<O> buffer) {
            buffer.addAll(seq);
        }

        @Override
        protected Tuple2<Chunk<O>, Chunk<O>> splitAtChunk_(final int n) {
            var s = size();
            var l = new ArrayList<O>(Math.min(s, n));
            var r = new ArrayList<O>(Math.max(0, s - n));
            int i = 0;
            for (; i < n; i++) {
                l.add(seq.get(i));
            }
            for (; i < s; i++) {
                r.add(seq.get(i));
            }
            return new Tuple2<>(Chunk.indexedSeq(l), Chunk.indexedSeq(r));
        }
    }
}
