package io.mitallast.concurrent;

import io.mitallast.higher.Higher;
import io.mitallast.io.Sync;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;
import io.mitallast.maybe.Maybe;
import io.mitallast.product.Tuple2;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public interface Ref<F extends Higher, A> {

    Higher<F, A> get();

    Higher<F, Unit> set(A a);

    Higher<F, A> getAndSet(A a);

    Higher<F, Tuple2<A, Function1<A, Higher<F, Boolean>>>> access();

    Higher<F, Boolean> tryUpdate(Function1<A, A> f);

    <B> Higher<F, Maybe<B>> tryModify(Function1<A, Tuple2<A, B>> f);

    Higher<F, Unit> update(Function1<A, A> f);

    <B> Higher<F, B> modify(Function1<A, Tuple2<A, B>> f);

    static <F extends Higher, A> Ref<F, A> unsafe(A a, Sync<F> F) {
        return new SyncRef<>(new AtomicReference<>(a), F);
    }
}

final class SyncRef<F extends Higher, A> implements Ref<F, A> {
    private final AtomicReference<A> ar;
    private final Sync<F> F;

    SyncRef(AtomicReference<A> ar, Sync<F> f) {
        this.ar = ar;
        F = f;
    }

    @Override
    public Higher<F, A> get() {
        return F.delay(ar::get);
    }

    @Override
    public Higher<F, Unit> set(A a) {
        return F.delay(() -> {
            ar.set(a);
            return Unit.unit();
        });
    }

    @Override
    public Higher<F, A> getAndSet(A a) {
        return F.delay(() -> ar.getAndSet(a));
    }

    @Override
    public Higher<F, Tuple2<A, Function1<A, Higher<F, Boolean>>>> access() {
        return F.delay(() -> {
            var snapshot = ar.get();
            var hasBeenCalled = new AtomicBoolean(false);
            return new Tuple2<A, Function1<A, Higher<F, Boolean>>>(snapshot, a -> F.delay(() -> hasBeenCalled.compareAndSet(false, true) && ar.compareAndSet(snapshot, a)));
        });
    }

    @Override
    public Higher<F, Boolean> tryUpdate(Function1<A, A> f) {
        return F.map(tryModify(a -> new Tuple2<>(f.apply(a), Unit.unit())), Maybe::isDefined);
    }

    @Override
    public <B> Higher<F, Maybe<B>> tryModify(Function1<A, Tuple2<A, B>> f) {
        return F.delay(() -> {
            var c = ar.get();
            var t = f.apply(c);
            var u = t.t1();
            var b = t.t2();
            if (ar.compareAndSet(c, u)) {
                return Maybe.some(b);
            } else {
                return Maybe.none();
            }
        });
    }

    @Override
    public Higher<F, Unit> update(Function1<A, A> f) {
        return modify(a -> new Tuple2<>(f.apply(a), Unit.unit()));
    }

    @Override
    public <B> Higher<F, B> modify(Function1<A, Tuple2<A, B>> f) {
        return F.delay(() -> {
            while (true) {
                var c = ar.get();
                var t = f.apply(c);
                var u = t.t1();
                var b = t.t2();
                if (ar.compareAndSet(c, u)) {
                    return b;
                }
            }
        });
    }
}