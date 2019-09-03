package io.mitallast.data;

import io.mitallast.categories.Applicative;
import io.mitallast.categories.FlatMap;
import io.mitallast.categories.Traverse;
import io.mitallast.categories.TraverseFilter;
import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.kernel.Eval;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Function2;
import io.mitallast.list.List;
import io.mitallast.maybe.Maybe;
import io.mitallast.product.Tuple2;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

public abstract class Chain<A> implements Higher<Chain, A> {

    public final Maybe<Tuple2<A, Chain<A>>> uncons() {
        Chain<A> c = this;
        var rights = new ArrayList<Chain<A>>();
        Maybe<Tuple2<A, Chain<A>>> result = null;
        while (result == null) {
            if (c instanceof Singleton) {
                var s = (Singleton<A>) c;
                if (rights.isEmpty()) {
                    result = Maybe.some(new Tuple2<>(s.a, empty()));
                } else {
                    var next = rights.stream().reduce(Chain::concat).get();
                    result = Maybe.some(new Tuple2<>(s.a, next));
                }
            } else if (c instanceof Append) {
                var s = (Append<A>) c;
                c = s.left;
                rights.add(s.right);
            } else if (c instanceof Wrap) {
                var s = (Wrap<A>) c;
                var tail = fromSeq(s.seq.tail());
                if (rights.isEmpty()) {
                    result = Maybe.some(new Tuple2<>(s.seq.head(), tail));
                } else {
                    var r = rights.stream().reduce(Chain::concat).get();
                    var next = concat(tail, r);
                    result = Maybe.some(new Tuple2<>(s.seq.head(), next));
                }
            } else if (c instanceof Empty) {
                result = Maybe.none();
            } else throw new IllegalArgumentException();
        }
        return result;
    }

    public abstract boolean isEmpty();

    public boolean nonEmpty() {
        return !isEmpty();
    }

    public Chain<A> append(Chain<A> c) {
        return concat(this, c);
    }

    public Chain<A> prepend(A a) {
        return concat(one(a), this);
    }

    public Chain<A> append(A a) {
        return concat(this, one(a));
    }

    public <B> Chain<B> map(Function1<A, B> f) {
        Chain<B> result = empty();
        var iter = iterator();
        while (iter.hasNext()) {
            result = result.append(f.apply(iter.next()));
        }
        return result;
    }

    public <B> Chain<B> flatMap(Function1<A, Chain<B>> f) {
        Chain<B> result = empty();
        var iter = iterator();
        while (iter.hasNext()) {
            result = concat(result, f.apply(iter.next()));
        }
        return result;
    }

    public <B> B foldLeft(B z, Function2<B, A, B> f) {
        var result = z;
        var iter = iterator();
        while (iter.hasNext()) {
            result = f.apply(result, iter.next());
        }
        return result;
    }

    public <B> B foldRight(B z, Function2<A, B, B> f) {
        var result = z;
        var iter = reverseIterator();
        while (iter.hasNext()) {
            result = f.apply(iter.next(), result);
        }
        return result;
    }

    public Chain<A> filter(Predicate<A> f) {
        Chain<A> result = empty();
        var iter = iterator();
        while (iter.hasNext()) {
            var a = iter.next();
            if (f.test(a)) {
                result = result.append(a);
            }
        }
        return result;
    }

    public Chain<A> filterNot(Predicate<A> f) {
        Chain<A> result = empty();
        var iter = iterator();
        while (iter.hasNext()) {
            var a = iter.next();
            if (!f.test(a)) {
                result = result.append(a);
            }
        }
        return result;
    }

    public Iterator<A> iterator() {
        if (this instanceof Wrap) {
            return ((Wrap<A>) this).seq.iterator();
        } else {
            return new ChainIterator<>(this);
        }
    }

    public Iterator<A> reverseIterator() {
        if (this instanceof Wrap) {
            return ((Wrap<A>) this).seq.reverse().iterator();
        } else {
            return new ChainReverseIterator<>(this);
        }
    }

    public Maybe<A> find(Predicate<A> f) {
        var iter = iterator();
        while (iter.hasNext()) {
            var a = iter.next();
            if (!f.test(a)) {
                return Maybe.some(a);
            }
        }
        return Maybe.none();
    }

    public boolean exists(Predicate<A> f) {
        var iter = iterator();
        while (iter.hasNext()) {
            var a = iter.next();
            if (!f.test(a)) {
                return true;
            }
        }
        return false;
    }

    public boolean forall(Predicate<A> f) {
        var iter = iterator();
        while (iter.hasNext()) {
            var a = iter.next();
            if (!f.test(a)) {
                return false;
            }
        }
        return true;
    }

    public <B, C> Chain<C> zipWith(Chain<B> other, Function2<A, B, C> f) {
        if (this.isEmpty() || other.isEmpty()) return empty();
        else {
            var iterA = iterator();
            var iterB = other.iterator();

            var result = one(f.apply(iterA.next(), iterB.next()));

            while (iterA.hasNext() && iterB.hasNext()) {
                result.append(f.apply(iterA.next(), iterB.next()));
            }
            return result;
        }
    }

    public Maybe<Tuple2<A, Chain<A>>> deleteFirst(Predicate<A> f) {
        var rem = this;
        var acc = Chain.<A>empty();
        while (true) {
            var u = rem.uncons();
            if (u.isDefined()) {
                var t = u.get();
                var a = t.t1();
                var tail = t.t2();
                if (!f.test(a)) {
                    rem = tail;
                    acc = acc.append(a);
                } else {
                    return Maybe.some(new Tuple2<>(a, acc.append(tail)));
                }
            } else {
                return Maybe.none();
            }
        }
    }

    public long length() {
        var iter = iterator();
        var i = 0L;
        while (iter.hasNext()) {
            i++;
            iter.next();
        }
        return i;
    }

    public List<A> toList() {
        var iter = iterator();
        List<A> i = List.empty();
        while (iter.hasNext()) {
            i = i.prepend(iter.next());
        }
        return i;
    }

    private Chain() {
    }

    private final static class Empty<A> extends Chain<A> {
        @Override
        public boolean isEmpty() {
            return true;
        }
    }

    private final static class Singleton<A> extends Chain<A> {
        private final A a;

        private Singleton(A a) {
            this.a = a;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    private final static class Append<A> extends Chain<A> {
        private final Chain<A> left;
        private final Chain<A> right;

        private Append(Chain<A> left, Chain<A> right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean isEmpty() {
            // b/c `concat` constructor doesn't allow either branch to be empty
            return false;
        }
    }

    private final static class Wrap<A> extends Chain<A> {
        private final List<A> seq;

        private Wrap(List<A> seq) {
            this.seq = seq;
        }

        @Override
        public boolean isEmpty() {
            // b/c `fromSeq` constructor doesn't allow either branch to be empty
            return false;
        }
    }

    public static <A> Chain<A> empty() {
        return new Empty<>();
    }

    public static <A> Chain<A> one(A a) {
        return new Singleton<>(a);
    }

    public static <A> Chain<A> concat(Chain<A> c, Chain<A> c2) {
        if (c.isEmpty()) return c2;
        else if (c2.isEmpty()) return c;
        else return new Append<>(c, c2);
    }

    public static <A> Chain<A> fromSeq(List<A> s) {
        if (s.isEmpty()) return empty();
        else if (s.size() == 1) return one(s.head());
        else return new Wrap<>(s);
    }

    public static <A> Chain<A> apply(A... as) {
        return fromSeq(List.of(as));
    }

    private final static class ChainIterator<A> implements Iterator<A> {
        private final ArrayList<Chain<A>> rights = new ArrayList<>();
        private final Chain<A> self;
        private Chain<A> c;
        private Iterator<A> currentIterator = null;

        private ChainIterator(Chain<A> self) {
            this.self = self;
            if (self.isEmpty()) {
                c = null;
            } else {
                c = self;
            }
        }

        @Override
        public boolean hasNext() {
            return (c != null) || ((currentIterator != null) && currentIterator.hasNext());
        }

        @Override
        public A next() {
            while (true) {
                if ((currentIterator != null) && currentIterator.hasNext()) {
                    return currentIterator.next();
                } else {
                    currentIterator = null;

                    if (c instanceof Singleton) {
                        var s = (Singleton<A>) c;
                        if (rights.isEmpty()) {
                            c = null;
                        } else {
                            c = rights.stream().reduce(Chain::concat).get();
                            rights.clear();
                        }
                        return s.a;
                    } else if (c instanceof Append) {
                        var s = (Append<A>) c;
                        c = s.left;
                        rights.add(s.right);
                    } else if (c instanceof Wrap) {
                        var s = (Wrap<A>) c;
                        if (rights.isEmpty()) {
                            c = null;
                        } else {
                            c = rights.stream().reduce(Chain::concat).get();
                            rights.clear();
                        }
                        currentIterator = s.seq.iterator();
                        return currentIterator.next();
                    } else {
                        throw new NoSuchElementException();
                    }
                }
            }
        }
    }

    private final static class ChainReverseIterator<A> implements Iterator<A> {
        private Chain<A> c;
        private final ArrayList<Chain<A>> lefts = new ArrayList<>();
        private Iterator<A> currentIterator = null;

        ChainReverseIterator(Chain<A> self) {
            if (self.isEmpty()) {
                c = null;
            } else {
                c = self;
            }
        }

        @Override
        public boolean hasNext() {
            return (c != null) || ((currentIterator != null) && currentIterator.hasNext());
        }

        @Override
        public A next() {
            while (true) {
                if (currentIterator != null && currentIterator.hasNext()) {
                    return currentIterator.next();
                } else {
                    currentIterator = null;

                    if (c instanceof Singleton) {
                        var cs = (Singleton<A>) c;
                        if (lefts.isEmpty()) {
                            c = null;
                        } else {
                            c = lefts.stream().reduce(Append::new).get();
                        }
                        lefts.clear();
                        return cs.a;
                    } else if (c instanceof Append) {
                        var ca = (Append<A>) c;
                        c = ca.right;
                        lefts.add(ca.left);
                    } else if (c instanceof Wrap) {
                        var cw = (Wrap<A>) c;
                        if (lefts.isEmpty()) {
                            c = null;
                        } else {
                            c = lefts.stream().reduce(Append::new).get();
                        }
                        lefts.clear();
                        currentIterator = cw.seq.reverse().iterator();
                        return currentIterator.next();
                    } else if (c == null || c instanceof Empty) {
                        throw new java.util.NoSuchElementException("next called on empty iterator");
                    }
                }
            }
        }
    }

    public static DataInstances instances() {
        return DataInstances.instance;
    }

    public static TraverseFilter<Chain> traverseFilter() {
        return ChainTraverseFilter.instance;
    }

    public static class DataInstances implements Traverse<Chain>, FlatMap<Chain> {
        private final static DataInstances instance = new DataInstances();

        @Override
        public <G extends Higher, A, B> Higher<G, Higher<Chain, B>> traverse(
            final Higher<Chain, A> fa,
            final Function1<A, Higher<G, B>> f,
            final Applicative<G> G
        ) {
            return ((Chain<A>) fa).foldRight(
                G.pure(Chain.empty()),
                (a, gcatb) -> G.map2(f.apply(a), gcatb, (aa, b) -> ((Chain<B>) b).append(aa))
            );
        }

        @Override
        public <A, B> B foldLeft(final Higher<Chain, A> fa, final B b, final Function2<B, A, B> f) {
            return ((Chain<A>) fa).foldLeft(b, f);
        }

        @Override
        public <A, B> Eval<B> foldRight(final Higher<Chain, A> fa, final Eval<B> lb, final Function2<A, Eval<B>, Eval<B>> f) {
            return Eval.defer(() -> ((Chain<A>) fa).foldRight(lb, (a, lbe) ->
                Eval.defer(() -> f.apply(a, lbe))
            ));
        }

        @Override
        public <A, B> Higher<Chain, B> map(final Higher<Chain, A> fa, final Function1<A, B> fn) {
            return ((Chain<A>) fa).map(fn);
        }

        @Override
        public <A, B> Higher<Chain, B> flatMap(final Higher<Chain, A> fa, final Function1<A, Higher<Chain, B>> f) {
            return ((Chain<A>) fa).flatMap(f.castUnsafe());
        }

        @Override
        public <A, B> Higher<Chain, B> tailRecM(final A a, final Function1<A, Higher<Chain, Either<A, B>>> f) {
            var acc = Chain.<B>empty();
            var rest = List.of((Chain<Either<A, B>>) f.apply(a));
            while (true) {
                if (rest.isEmpty()) {
                    return acc;
                } else {
                    var unc = rest.head().uncons();
                    if (unc.isDefined()) {
                        var hdh = unc.get().t1();
                        var hdt = unc.get().t2();
                        if (hdh.isRight()) {
                            var b = hdh.right().get();
                            acc = acc.prepend(b);
                            rest = rest.tail().prepend(hdt);
                        } else {
                            var aa = hdh.left().get();
                            rest = rest.tail().prepend(hdt).prepend((Chain<Either<A, B>>) f.apply(aa));
                        }
                    } else {
                        rest = rest.tail();
                    }
                }
            }
        }
    }

    private static class ChainTraverseFilter implements TraverseFilter<Chain> {
        private static final ChainTraverseFilter instance = new ChainTraverseFilter();

        @Override
        public <G extends Higher, A, B> Higher<G, Higher<Chain, B>> traverseFilter(
            final Higher<Chain, A> fa,
            final Function1<A, Higher<G, Maybe<B>>> f,
            final Applicative<G> G) {
            return ((Chain<A>) fa).foldRight(
                G.pure(Chain.<B>empty()),
                (a, gcb) -> G.map2(
                    f.apply(a),
                    gcb,
                    (ob, cb) -> ob.fold(() -> (Chain<B>) cb, ((Chain<B>) cb)::append)
                )
            );
        }
    }
}
