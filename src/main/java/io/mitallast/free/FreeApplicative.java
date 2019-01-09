package io.mitallast.free;

import io.mitallast.categories.Applicative;
import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Function2;
import io.mitallast.product.Tuple2;

public abstract class FreeApplicative<F extends Higher, A> implements Higher<FreeApplicative<F, ?>, A> {
    private FreeApplicative() {
    }

    final public <B> FreeApplicative<F, B> ap(FreeApplicative<F, Function1<A, B>> b) {
        if (b instanceof Pure) {
            return map(((Pure<F, Function1<A, B>>) b).a);
        } else {
            return new Ap<>(b, this);
        }
    }

    final public <B> FreeApplicative<F, B> map(Function1<A, B> f) {
        if (this instanceof Pure) {
            return new Pure<>(f.apply(((Pure<F, A>) this).a));
        } else {
            return new Ap<>(new Pure<>(f), this);
        }
    }

    final public <B, C> FreeApplicative<F, C> map2(FreeApplicative<F, B> fb, Function2<A, B, C> f) {
        if (this instanceof Pure) {
            return fb.map(b -> f.apply(((Pure<F, A>) this).a, b));
        } else {
            if (fb instanceof Pure) {
                return new Ap<>(new Pure<>(a -> f.apply(a, ((Pure<F, B>) fb).a)), this);
            } else {
                return new Ap<>(new Ap<>(new Pure<>(a -> b -> f.apply(a, b)), this), fb);
            }
        }
    }

    @Override
    public String toString() {
        return "FreeApplicative(...)";
    }

    private final static class Fn<G extends Higher, A, B> {
        private final Higher<G, Function1<A, B>> gab;
        private final int argc;

        private Fn(Higher<G, Function1<A, B>> gab, int argc) {
            this.gab = gab;
            this.argc = argc;
        }
    }

    private final static class Pure<F extends Higher, A> extends FreeApplicative<F, A> {
        private final A a;

        private Pure(A a) {
            this.a = a;
        }
    }

    private final static class Lift<F extends Higher, A> extends FreeApplicative<F, A> {
        private final Higher<F, A> fa;

        private Lift(Higher<F, A> fa) {
            this.fa = fa;
        }
    }

    private final static class Ap<F extends Higher, P, A> extends FreeApplicative<F, A> {
        private final FreeApplicative<F, Function1<P, A>> fn;
        private final FreeApplicative<F, P> fp;

        private Ap(FreeApplicative<F, Function1<P, A>> fn, FreeApplicative<F, P> fp) {
            this.fn = fn;
            this.fp = fp;
        }
    }

    static <F extends Higher, A> FreeApplicative<F, A> pure(A a) {
        return new Pure<>(a);
    }

    static <F extends Higher, P, A> FreeApplicative<F, A> ap(Higher<F, P> fp, FreeApplicative<F, Function1<P, A>> f) {
        return new Ap<>(f, new Lift<>(fp));
    }

    static <F extends Higher, A> FreeApplicative<F, A> lift(Higher<F, A> fa) {
        return new Lift<>(fa);
    }

    static <S extends Higher> Applicative<FreeApplicative<S, ?>> applicative() {
        return new Applicative<>() {
            @Override
            public <A, B> Higher<FreeApplicative<S, ?>, Tuple2<A, B>> product(Higher<FreeApplicative<S, ?>, A> fa,
                                                                              Higher<FreeApplicative<S, ?>, B> fb) {
                return map2(fa, fb, Tuple2::new);
            }

            @Override
            public <A, B> Higher<FreeApplicative<S, ?>, B> map(Higher<FreeApplicative<S, ?>, A> fa,
                                                               Function1<A, B> f) {
                return ((FreeApplicative<S, A>) fa).map(f);
            }

            @Override
            public <A, B> Higher<FreeApplicative<S, ?>, B> ap(Higher<FreeApplicative<S, ?>, Function1<A, B>> ff, Higher<FreeApplicative<S, ?>, A> fa) {
                return ((FreeApplicative<S, A>) fa).ap((FreeApplicative<S, Function1<A, B>>) ff);
            }

            @Override
            public <A> Higher<FreeApplicative<S, ?>, A> pure(A a) {
                return new Pure<>(a);
            }

            @Override
            public <A, B, Z> Higher<FreeApplicative<S, ?>, Z> map2(Higher<FreeApplicative<S, ?>, A> hfa,
                                                                   Higher<FreeApplicative<S, ?>, B> hfb,
                                                                   Function2<A, B, Z> f) {
                var fa = (FreeApplicative<S, A>) hfa;
                var fb = (FreeApplicative<S, B>) hfb;
                return fa.map2(fb, f);
            }
        };
    }
}
