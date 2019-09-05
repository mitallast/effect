package io.mitallast.free;

import io.mitallast.arrow.FunctionK;
import io.mitallast.categories.*;
import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.io.Defer;
import io.mitallast.kernel.Eval;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Function2;
import io.mitallast.lambda.Supplier;
import io.mitallast.maybe.Maybe;

import java.net.HttpURLConnection;
import java.util.function.Function;

public abstract class Free<S extends Higher, A> implements Higher<Free<S, ?>, A> {

    public final <B> Free<S, B> map(Function1<A, B> f) {
        return flatMap(a -> pure(f.apply(a)));
    }

    public final <B> Free<S, B> flatMap(Function1<A, Free<S, B>> f) {
        return new FlatMapped<>(this, f);
    }

    public final <B> B fold(Function1<A, B> r,
                            Function1<Higher<S, Free<S, A>>, B> s,
                            Functor<S> S) {
        return resume(S).fold(s, r);
    }

    /**
     * Takes one evaluation step in the Free monad, re-associating left-nested binds in the process.
     */
    public abstract Free<S, A> step();

    /**
     * Evaluate a single layer of the free monad.
     */
    public abstract Either<Higher<S, Free<S, A>>, A> resume(Functor<S> S);

    /**
     * A combination of step and fold.
     */
    protected abstract <B, X> B foldStep(Function1<A, B> onPure,
                                         Function1<Higher<S, A>, B> onSuspend,
                                         Function2<Higher<S, X>, Function1<X, Free<S, A>>, B> onFlatMapped);

    /**
     * Run to completion, using a function that extracts the resumption
     * from its suspension functor.
     */
    public final A go(Function1<Higher<S, Free<S, A>>, Free<S, A>> f, Functor<S> S) {
        Free<S, A> t = this;
        do {
            var e = t.resume(S);
            if (e.isLeft()) {
                t = f.apply(e.left().get());
            } else {
                return e.right().get();
            }
        } while (true);
    }

    /**
     * Run to completion, using the given comonad to extract the
     * resumption.
     */
    public final A run(Comonad<S> S) {
        return go(S::extract, S);
    }

    /**
     * Run to completion, using a function that maps the resumption
     * from `S` to a monad `M`.
     */
    public final <M extends Higher> Higher<M, A> runM(Function1<Higher<S, Free<S, A>>, Higher<M, Free<S, A>>> f,
                                                      Functor<S> S,
                                                      Monad<M> M) {
        return resume(S).fold(
            s -> M.tailRecM(s, t -> M.map(f.apply(t), ff -> ff.resume(S))),
            r -> M.pure(r)
        );
    }

    /**
     * Run to completion, using monadic recursion to evaluate the
     * resumption in the context of `S`.
     */
    public final Higher<S, A> runTailRec(Monad<S> S) {
        var step = new Function1<Free<S, A>, Higher<S, Either<Free<S, A>, A>>>() {

            @Override
            public Higher<S, Either<Free<S, A>, A>> apply(Free<S, A> rma) {
                if (rma instanceof Pure) {
                    var pure = (Pure<S, A>) rma;
                    return S.pure(Either.right(pure.a));
                } else if (rma instanceof Suspend) {
                    var suspend = (Suspend<S, A>) rma;
                    return S.map(suspend.a, Either::right);
                } else {
                    var fm = (FlatMapped<S, A, Object>) rma;
                    var curr = fm.c;
                    var f = fm.f;
                    if (curr instanceof Pure) {
                        var pure = (Pure<S, Object>) curr;
                        return S.pure(Either.left(f.apply(pure.a)));
                    } else if (curr instanceof Suspend) {
                        var suspend = (Suspend<S, Object>) curr;
                        return S.map(suspend.a, x -> Either.left(f.apply(x)));
                    } else {
                        var fmm = (FlatMapped<S, Object, Object>) curr;
                        var prev = fmm.c;
                        var g = fmm.f;
                        return S.pure(Either.left(prev.flatMap(w -> g.apply(w).flatMap(f))));
                    }
                }
            }
        };
        return S.tailRecM(this, step);
    }

    /**
     * Catamorphism for `Free`.
     * <p>
     * Run to completion, mapping the suspension with the given
     * transformation at each step and accumulating into the monad `M`.
     * <p>
     * This method uses `tailRecM` to provide stack-safety.
     */
    public final <M extends Higher> Higher<M, A> foldMap(FunctionK<S, M> f,
                                                         Monad<M> M) {
        return M.tailRecM(this, x -> {
            var s = x.step();
            if (s instanceof Pure) {
                var p = (Pure<S, A>) s;
                return M.pure(Either.right(p.a));
            } else if (s instanceof Suspend) {
                var suspend = (Suspend<S, A>) s;
                return M.map(f.apply(suspend.a), Either::right);
            } else {
                var fm = (FlatMapped<S, A, A>) s;
                var c = fm.c;
                var g = fm.f;
                return M.map(c.foldMap(f, M), cc -> Either.left(g.apply(cc)));
            }
        });
    }

    /**
     * Return from the computation with the given value.
     */
    private static final class Pure<S extends Higher, A> extends Free<S, A> {
        private final A a;

        private Pure(A a) {
            this.a = a;
        }

        @Override
        public Free<S, A> step() {
            return this;
        }

        @Override
        public Either<Higher<S, Free<S, A>>, A> resume(Functor<S> S) {
            return Either.right(a);
        }

        @Override
        protected <B, X> B foldStep(Function1<A, B> onPure,
                                    Function1<Higher<S, A>, B> onSuspend,
                                    Function2<Higher<S, X>, Function1<X, Free<S, A>>, B> onFlatMapped) {
            return onPure.apply(a);
        }
    }

    /**
     * Suspend the computation with the given suspension.
     */
    private static final class Suspend<S extends Higher, A> extends Free<S, A> {
        private final Higher<S, A> a;

        private Suspend(Higher<S, A> a) {
            this.a = a;
        }

        @Override
        public Free<S, A> step() {
            return this;
        }

        @Override
        public Either<Higher<S, Free<S, A>>, A> resume(Functor<S> S) {
            return Either.left(S.map(a, Free::pure));
        }

        @Override
        protected <B, X> B foldStep(Function1<A, B> onPure,
                                    Function1<Higher<S, A>, B> onSuspend,
                                    Function2<Higher<S, X>, Function1<X, Free<S, A>>, B> onFlatMapped) {
            return onSuspend.apply(a);
        }
    }

    /**
     * Call a subroutine and continue with the given function.
     */
    private static final class FlatMapped<S extends Higher, B, C> extends Free<S, B> {
        private final Free<S, C> c;
        private final Function1<C, Free<S, B>> f;

        private FlatMapped(Free<S, C> c, Function1<C, Free<S, B>> f) {
            this.c = c;
            this.f = f;
        }

        @Override
        public Free<S, B> step() {
            if (c instanceof Pure) {
                var p = (Pure<S, C>) c;
                return f.apply(p.a).step();
            } else if (c instanceof FlatMapped) {
                var fm = (FlatMapped<S, C, Object>) c;
                return fm.c.flatMap(cc -> fm.f.apply(cc).flatMap(f)).step();
            } else {
                return this;
            }
        }

        @Override
        public Either<Higher<S, Free<S, B>>, B> resume(Functor<S> S) {
            if (c instanceof Pure) {
                var pure = (Pure<S, C>) c;
                return f.apply(pure.a).resume(S);
            } else if (c instanceof Suspend) {
                var suspend = (Suspend<S, C>) c;
                return Either.left(S.map(suspend.a, f));
            } else {
                var flatMapped = (FlatMapped<S, C, Object>) c;
                return flatMapped.c.flatMap(dd -> flatMapped.f.apply(dd).flatMap(f)).resume(S);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        protected <BB, X> BB foldStep(Function1<B, BB> onPure,
                                      Function1<Higher<S, B>, BB> onSuspend,
                                      Function2<Higher<S, X>, Function1<X, Free<S, B>>, BB> onFlatMapped) {
            if (c instanceof Suspend) {
                var suspend = (Suspend<S, X>) c;
                return onFlatMapped.apply(suspend.a, x -> f.apply((C) x));
            } else throw new IllegalStateException("FlatMapped should be right associative after step");
        }
    }

    /**
     * Lift a pure `A` value into the free monad.
     */
    public static <S extends Higher, A> Free<S, A> pure(A a) {
        return new Pure<>(a);
    }

    /**
     * Lift an `F[A]` value into the free monad.
     */
    public static <S extends Higher, A> Free<S, A> liftF(Higher<S, A> value) {
        return new Suspend<>(value);
    }

    /**
     * Absorb a step into the free monad.
     */
    public static <S extends Higher, A> Free<S, A> roll(Higher<S, Free<S, A>> value) {
        return liftF(value).flatMap(s -> s);
    }

    /**
     * Defer the creation of a `Free[F, A]` value.
     */
    public static <F extends Higher, A> Free<F, A> defer(Supplier<Free<F, A>> value) {
        return Free.<F, Unit>pure(Unit.unit()).flatMap(u -> value.get());
    }

    public static <S extends Higher> Monad<Free<S, ?>> monad() {
        return new FreeMonad<>();
    }

    public static <S extends Higher> Defer<Free<S, ?>> defer() {
        return new FreeDefer<>();
    }

    public static <S extends Higher> Foldable<Free<S, ?>> foldable(Foldable<S> foldable) {
        return (FreeFoldable<S>) () -> foldable;
    }

    public static <S extends Higher> Traverse<Free<S, ?>> traverse(Traverse<S> traverse) {
        return (FreeTraverse<S>) () -> traverse;
    }
}

interface FreeFoldable<F extends Higher> extends Foldable<Free<F, ?>> {
    Foldable<F> F();

    @Override
    default <A, B> B foldLeft(Higher<Free<F, ?>, A> fa, B b, Function2<B, A, B> f) {
        var fra = (Free<F, A>) fa;
        return fra.foldStep(
            a -> f.apply(b, a),
            ffa -> F().foldLeft(ffa, b, f),
            (fx, g) -> F().foldLeft(fx, b, (bb, x) -> foldLeft(g.apply(x), bb, f))
        );
    }

    @Override
    default <A, B> Eval<B> foldRight(Higher<Free<F, ?>, A> hfa, Eval<B> lb, Function2<A, Eval<B>, Eval<B>> f) {
        var fa = (Free<F, A>) hfa;
        return fa.<Eval<B>, A>foldStep(
            a -> f.apply(a, lb),
            ffa -> F().foldRight(ffa, lb, f),
            (fx, g) -> F().foldRight(fx, lb, (a, lbb) -> foldRight(g.apply(a), lbb, f))
        );
    }
}

interface FreeTraverse<F extends Higher> extends Traverse<Free<F, ?>>, FreeFoldable<F> {
    @Override
    Traverse<F> F();

    @SuppressWarnings("unchecked")
    @Override
    default <G extends Higher, A, B>
    Higher<G, Higher<Free<F, ?>, B>> traverse(Higher<Free<F, ?>, A> hfa,
                                              Function1<A, Higher<G, B>> f,
                                              Applicative<G> G) {
        var fa = (Free<F, A>) hfa;
        return fa.resume(F()).fold(
            freeA -> G.map(F().traverse(freeA, ffa -> traverse(ffa, f, G), G), hffb -> {
                var h = (Higher<F, ?>) hffb;
                var ffb = (Higher<F, Free<F, B>>) h;
                return Free.roll(ffb);
            }),
            a -> G.map(f.apply(a), Free::pure)
        );
    }

    @Override
    default <A, B>
    Higher<Free<F, ?>, B> map(Higher<Free<F, ?>, A> hfa, Function1<A, B> f) {
        var fa = (Free<F, A>) hfa;
        return fa.map(f);
    }
}

class FreeMonad<F extends Higher> implements StackSafeMonad<Free<F, ?>> {
    @Override
    public <A> Free<F, A> pure(A a) {
        return Free.pure(a);
    }

    private final Higher<Free<F, ?>, Unit> unit = Free.pure(Unit.unit());
    private final Higher<Free<F, ?>, Maybe<?>> none = Free.pure(Maybe.none());

    @Override
    public Higher<Free<F, ?>, Unit> unit() {
        return unit;
    }

    @Override
    public <A> Higher<Free<F, ?>, Maybe<A>> none() {
        return none.castTUnsafe();
    }

    @Override
    public <A, B> Free<F, B> map(Higher<Free<F, ?>, A> fa, Function1<A, B> f) {
        return ((Free<F, A>) fa).map(f);
    }

    @Override
    public <A, B> Free<F, B> flatMap(Higher<Free<F, ?>, A> fa, Function1<A, Higher<Free<F, ?>, B>> f) {
        return ((Free<F, A>) fa).flatMap(f.cast());
    }
}

class FreeDefer<S extends Higher> implements Defer<Free<S, ?>> {

    @Override
    public <A> Higher<Free<S, ?>, A> defer(Supplier<Higher<Free<S, ?>, A>> fa) {
        return Free.defer(fa.cast());
    }
}