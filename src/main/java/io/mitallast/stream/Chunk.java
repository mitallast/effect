package io.mitallast.stream;

import io.mitallast.data.Queue;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Function2;
import io.mitallast.list.List;
import io.mitallast.maybe.Maybe;
import io.mitallast.product.Tuple;
import io.mitallast.product.Tuple2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Predicate;

abstract class Chunk<O> implements Iterable<O> {

    private Chunk() {
    }

    abstract public int size();

    abstract public O apply(int i);

    final public Chunk<O> take(int n) {
        return splitAt(n).t1();
    }

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
        return Tuple.of(acc, new IndexedSeqChunk<>(b));
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
        if (n <= 0) return Tuple.of(Chunk.empty(), this);
        else if (n >= size()) return Tuple.of(this, Chunk.empty());
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

    public static <A> Chunk<A> concat(Iterable<Chunk<A>> chunks) {
        var size = 0;
        for (Chunk<A> chunk : chunks) {
            size += chunk.size();
        }
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
            return Tuple.of(Chunk.indexedSeq(l), Chunk.indexedSeq(r));
        }
    }

    final static class CQueue<A> {
        final Queue<Chunk<A>> chunks;
        final int size;

        private CQueue(final Queue<Chunk<A>> chunks, final int size) {
            this.chunks = chunks;
            this.size = size;
        }

        public Iterator<A> iterator() {
            return new Iterator<A>() {
                final Iterator<Chunk<A>> chunksIterator = chunks.iterator();
                Iterator<A> chunkIterator = null;

                @Override
                public boolean hasNext() {
                    while (true) {
                        if (chunkIterator != null) {
                            if (chunkIterator.hasNext()) {
                                return true;
                            } else {
                                chunkIterator = null;
                            }
                        } else {
                            if (chunksIterator.hasNext()) {
                                chunkIterator = chunksIterator.next().iterator();
                            } else {
                                return false;
                            }
                        }
                    }
                }

                @Override
                public A next() {
                    if (hasNext()) {
                        assert chunkIterator != null;
                        return chunkIterator.next();
                    } else throw new NoSuchElementException();
                }
            };
        }

        public CQueue<A> prepend(Chunk<A> c) {
            return new CQueue<>(chunks.prepend(c), c.size() + size);
        }

        public CQueue<A> append(Chunk<A> c) {
            return new CQueue<>(chunks.append(c), c.size() + size);
        }

        public CQueue<A> take(int n) {
            if (n <= 0) return empty();
            else if (n >= size) return this;
            else {
                var acc = Queue.<Chunk<A>>empty();
                var rem = chunks;
                var toTake = n;
                while (true) {
                    var t = rem.dequeue();
                    var next = t.t1();
                    var tail = t.t2();
                    var nextSize = next.size();
                    if (nextSize < toTake) {
                        acc = acc.prepend(next);
                        rem = tail;
                        toTake = toTake - nextSize;
                    } else {
                        return new CQueue<>(acc.append(next.take(toTake)), n);
                    }
                }
            }
        }

        public CQueue<A> takeRight(int n) {
            if (n <= 0) return empty();
            else return drop(size - n);
        }

        public CQueue<A> drop(int n) {
            if (n <= 0) return this;
            else if (n >= size) return empty();
            else {
                var rem = chunks;
                var toDrop = n;
                while (true) {
                    if (toDrop <= 0) return new CQueue<>(rem, size - n);
                    else {
                        var next = rem.head();
                        var nextSize = next.size();
                        if (nextSize < toDrop) {
                            rem = rem.tail();
                            toDrop = toDrop - nextSize;
                        } else if (nextSize == toDrop) {
                            return new CQueue<>(rem.tail(), size - n);
                        } else {
                            return new CQueue<>(rem.tail().append(next.drop(toDrop)), size - n);
                        }
                    }
                }
            }
        }

        public CQueue<A> dropRight(int n) {
            if (n <= 0) return this;
            else return take(size - n);
        }

        public Chunk<A> toChunk() {
            return Chunk.concat(chunks);
        }

        private final static CQueue empty = new CQueue<>(Queue.empty(), 0);

        @SuppressWarnings("unchecked")
        public static <A> CQueue<A> empty() {
            return (CQueue<A>) empty;
        }

        @SafeVarargs
        public static <A> CQueue<A> apply(Chunk<A>... chunks) {
            var acc = CQueue.<A>empty();
            for (Chunk<A> chunk : chunks) {
                acc = acc.append(chunk);
            }
            return acc;
        }
    }
}
