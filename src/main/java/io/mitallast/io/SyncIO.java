package io.mitallast.io;

import io.mitallast.categories.StackSafeMonad;
import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.kernel.Monoid;
import io.mitallast.kernel.Semigroup;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class SyncIO<A> implements Higher<SyncIO, A> {
    private final IO<A> toIO;

    public SyncIO(IO<A> toIO) {
        this.toIO = toIO;
    }

    public A unsafeRunSync() {
        return toIO.unsafeRunSync();
    }

    public <B> SyncIO<B> map(Function1<A, B> f) {
        return new SyncIO<>(toIO.map(f));
    }

    public <B> SyncIO<B> flatMap(Function1<A, SyncIO<B>> f) {
        return new SyncIO<>(toIO.flatMap(a -> f.apply(a).toIO));
    }

    public SyncIO<Either<Throwable, A>> attempt() {
        return new SyncIO<>(toIO.attempt());
    }

    public <F extends Higher> Higher<F, A> to(LiftIO<F> F) {
        return F.liftIO(toIO);
    }

    public <B> SyncIO<B> bracket(Function1<A, SyncIO<B>> use, Function1<A, SyncIO<Unit>> release) {
        return bracketCase(use, (a, ec) -> release.apply(a));
    }

    public <B> SyncIO<B> bracketCase(Function1<A, SyncIO<B>> use, BiFunction<A, ExitCase<Throwable>, SyncIO<Unit>> release) {
        return new SyncIO<>(toIO.bracketCase(a -> use.apply(a).toIO, (a, ec) -> release.apply(a, ec).toIO));
    }

    public SyncIO<A> guarantee(final SyncIO<Unit> finalizer) {
        return guaranteeCase(ec -> finalizer);
    }

    public SyncIO<A> guaranteeCase(final Function1<ExitCase<Throwable>, SyncIO<Unit>> finalizer) {
        return new SyncIO<>(toIO.guaranteeCase(ec -> finalizer.apply(ec).toIO));
    }

    public SyncIO<A> handleErrorWith(Function1<Throwable, SyncIO<A>> f) {
        return new SyncIO<>(toIO.handleErrorWith(t -> f.apply(t).toIO));
    }

    public <B> SyncIO<B> redeem(Function1<Throwable, B> recover, Function1<A, B> map) {
        return new SyncIO<>(toIO.redeem(recover, map));
    }

    public <B> SyncIO<B> redeemWith(Function1<Throwable, IO<B>> recover, Function1<A, IO<B>> map) {
        return new SyncIO<>(toIO.redeemWith(recover, map));
    }

    public static <A> SyncIO<A> apply(Supplier<A> thunk) {
        return new SyncIO<>(IO.apply(thunk));
    }

    public static <A> SyncIO<A> suspend(Supplier<SyncIO<A>> thunk) {
        return new SyncIO<>(IO.suspend(() -> thunk.get().toIO));
    }

    public static <A> SyncIO<A> pure(A a) {
        return new SyncIO<>(IO.pure(a));
    }

    public static SyncIO<Unit> unit() {
        return new SyncIO<>(IO.unit());
    }

    public static <A> SyncIO<A> raiseError(Throwable e) {
        return new SyncIO<>(IO.raiseError(e));
    }

    public static <A> SyncIO<A> fromEither(Either<Throwable, A> e) {
        return new SyncIO<>(IO.fromEither(e));
    }

    public static Sync<SyncIO> sync() {
        return SyncIOSync.instance;
    }

    public static <A> Monoid<SyncIO<A>> monoid(Monoid<A> monoid) {
        return new SyncIOMonoid<>(monoid);
    }
}

class SyncIOSync implements Sync<SyncIO>, StackSafeMonad<SyncIO> {
    public static final SyncIOSync instance = new SyncIOSync();

    private SyncIOSync() {
    }

    @Override
    public <A> Higher<SyncIO, A> pure(A a) {
        return SyncIO.pure(a);
    }

    @Override
    public Higher<SyncIO, Unit> unit() {
        return SyncIO.unit();
    }

    @Override
    public <A, B> Higher<SyncIO, B> map(Higher<SyncIO, A> fa, Function1<A, B> f) {
        return ((SyncIO<A>) fa).map(f);
    }

    @Override
    public <A, B> Higher<SyncIO, B> flatMap(Higher<SyncIO, A> fa, Function1<A, Higher<SyncIO, B>> f) {
        return ((SyncIO<A>) fa).flatMap(a -> (SyncIO<B>) f.apply(a));
    }

    @Override
    public <A> Higher<SyncIO, Either<Throwable, A>> attempt(Higher<SyncIO, A> fa) {
        return ((SyncIO<A>) fa).attempt();
    }

    @Override
    public <A> Higher<SyncIO, A> handleErrorWith(Higher<SyncIO, A> fa, Function1<Throwable, Higher<SyncIO, A>> f) {
        return ((SyncIO<A>) fa).handleErrorWith(e -> (SyncIO<A>) f.apply(e));
    }

    @Override
    public <A> Higher<SyncIO, A> raiseError(Throwable e) {
        return SyncIO.raiseError(e);
    }

    @Override
    public <A, B> Higher<SyncIO, B> bracket(Higher<SyncIO, A> acquire, Function1<A, Higher<SyncIO, B>> use, Function1<A, Higher<SyncIO, Unit>> release) {
        return ((SyncIO<A>) acquire).bracket(a -> (SyncIO<B>) use.apply(a), a -> (SyncIO<Unit>) release.apply(a));
    }

    @Override
    public <A, B> Higher<SyncIO, B> bracketCase(Higher<SyncIO, A> acquire, Function1<A, Higher<SyncIO, B>> use, BiFunction<A, ExitCase<Throwable>, Higher<SyncIO, Unit>> release) {
        return ((SyncIO<A>) acquire).bracketCase(a -> (SyncIO<B>) use.apply(a), (a, ec) -> (SyncIO<Unit>) release.apply(a, ec));
    }

    @Override
    public <A> Higher<SyncIO, A> uncancelable(Higher<SyncIO, A> fa) {
        return fa;
    }

    @Override
    public <A> Higher<SyncIO, A> guarantee(Higher<SyncIO, A> fa, Higher<SyncIO, Unit> finalizer) {
        return ((SyncIO<A>) fa).guarantee((SyncIO<Unit>) finalizer);
    }

    @Override
    public <A> Higher<SyncIO, A> guaranteeCase(Higher<SyncIO, A> fa, Function1<ExitCase<Throwable>, Higher<SyncIO, Unit>> finalizer) {
        return ((SyncIO<A>) fa).guaranteeCase(ec -> (SyncIO<Unit>) finalizer.apply(ec));
    }

    @Override
    public <A> Higher<SyncIO, A> delay(Supplier<A> thunk) {
        return SyncIO.apply(thunk);
    }

    @Override
    public <A> Higher<SyncIO, A> suspend(Supplier<Higher<SyncIO, A>> thunk) {
        return SyncIO.suspend(() -> (SyncIO<A>) thunk.get());
    }
}

class SyncIOSemigroup<A> implements Semigroup<SyncIO<A>> {
    private final Semigroup<A> semigroup;

    SyncIOSemigroup(Semigroup<A> semigroup) {
        this.semigroup = semigroup;
    }

    @Override
    public SyncIO<A> combine(SyncIO<A> sioa1, SyncIO<A> sioa2) {
        return sioa1.flatMap(a1 -> sioa2.map(a2 -> semigroup.combine(a1, a2)));
    }
}

class SyncIOMonoid<A> extends SyncIOSemigroup<A> implements Monoid<SyncIO<A>> {
    private final Monoid<A> monoid;

    SyncIOMonoid(Monoid<A> monoid) {
        super(monoid);
        this.monoid = monoid;
    }

    @Override
    public SyncIO<A> empty() {
        return SyncIO.pure(monoid.empty());
    }
}