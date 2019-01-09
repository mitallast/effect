package io.mitallast.kernel;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Supplier;
import io.mitallast.list.List;
import io.mitallast.maybe.Maybe;

/**
 * Eval is a monad which controls evaluation.
 * <p>
 * This type wraps a value (or a computation that produces a value)
 * and can produce it on command via the `.value` method.
 * <p>
 * There are three basic evaluation strategies:
 * <p>
 * - Now:    evaluated immediately
 * - Later:  evaluated once when value is needed
 * - Always: evaluated every time value is needed
 * <p>
 * The Later and Always are both lazy strategies while Now is eager.
 * Later and Always are distinguished from each other only by
 * memoization: once evaluated Later will save the value to be returned
 * immediately if it is needed again. Always will run its computation
 * every time.
 * <p>
 * Eval supports stack-safe lazy computation via the .map and .flatMap
 * methods, which use an internal trampoline to avoid stack overflows.
 * Computation done within .map and .flatMap is always done lazily,
 * even when applied to a Now instance.
 * <p>
 * It is not generally good style to pattern-match on Eval instances.
 * Rather, use .map and .flatMap to chain computation, and use .value
 * to get the result when needed. It is also not good style to create
 * Eval instances whose computation involves calling .value on another
 * Eval instance -- this can defeat the trampolining and lead to stack
 * overflows.
 */
public abstract class Eval<A> implements Higher<Eval, A> {

    /**
     * Evaluate the computation and return an A value.
     * <p>
     * For lazy instances (Later, Always), any necessary computation
     * will be performed at this point. For eager instances (Now), a
     * value will be immediately returned.
     */
    public abstract A value();

    /**
     * Transform an Eval[A] into an Eval[B] given the transformation
     * function `f`.
     * <p>
     * This call is stack-safe -- many .map calls may be chained without
     * consumed additional stack during evaluation.
     * <p>
     * Computation performed in f is always lazy, even when called on an
     * eager (Now) instance.
     */
    public final <B> Eval<B> map(Function1<A, B> f) {
        return flatMap(a -> new Now<>(f.apply(a)));
    }

    /**
     * Lazily perform a computation based on an Eval[A], using the
     * function `f` to produce an Eval[B] given an A.
     * <p>
     * This call is stack-safe -- many .flatMap calls may be chained
     * without consumed additional stack during evaluation. It is also
     * written to avoid left-association problems, so that repeated
     * calls to .flatMap will be efficiently applied.
     * <p>
     * Computation performed in f is always lazy, even when called on an
     * eager (Now) instance.
     */
    public <B> Eval<B> flatMap(Function1<A, Eval<B>> f) {
        final var self = this;
        return new FlatMap<B, A>() {

            @Override
            public Supplier<Eval<A>> start() {
                return () -> self;
            }

            @Override
            public Eval<B> run(A a) {
                return f.apply(a);
            }
        };
    }

    /**
     * Ensure that the result of the computation (if any) will be
     * memoized.
     * <p>
     * Practically, this means that when called on an Always[A] a
     * Later[A] with an equivalent computation will be returned.
     */
    public abstract Eval<A> memoize();

    /**
     * Construct an eager Eval[A] instance.
     * <p>
     * In some sense it is equivalent to using a val.
     * <p>
     * This type should be used when an A value is already in hand, or
     * when the computation to produce an A value is pure and very fast.
     */
    public static final class Now<A> extends Eval<A> {
        private final A value;

        private Now(A value) {
            this.value = value;
        }

        @Override
        public A value() {
            return value;
        }

        @Override
        public Eval<A> memoize() {
            return this;
        }
    }

    /**
     * Construct a lazy Eval[A] instance.
     * <p>
     * This type should be used for most "lazy" values. In some sense it
     * is equivalent to using a lazy val.
     * <p>
     * When caching is not required or desired (e.g. if the value produced
     * may be large) prefer Always. When there is no computation
     * necessary, prefer Now.
     * <p>
     * Once Later has been evaluated, the closure (and any values captured
     * by the closure) will not be retained, and will be available for
     * garbage collection.
     */
    public static final class Later<A> extends Eval<A> {
        private Supplier<A> thunk;
        private A value;


        private Later(Supplier<A> thunk) {
            this.thunk = thunk;
        }

        @Override
        public A value() {
            // The idea here is that `f` may have captured very large
            // structures, but produce a very small result. In this case, once
            // we've calculated a value, we would prefer to be able to free
            // everything else.
            //
            // (For situations where `f` is small, but the output will be very
            // expensive to store, consider using `Always`.)
            if (thunk != null) {
                value = thunk.get();
                thunk = null;
            }
            return value;
        }

        @Override
        public Eval<A> memoize() {
            return this;
        }
    }

    /**
     * Construct a lazy Eval[A] instance.
     * <p>
     * This type can be used for "lazy" values. In some sense it is
     * equivalent to using a Function0 value.
     * <p>
     * This type will evaluate the computation every time the value is
     * required. It should be avoided except when laziness is required and
     * caching must be avoided. Generally, prefer Later.
     */
    public static final class Always<A> extends Eval<A> {
        private final Supplier<A> thunk;

        private Always(Supplier<A> thunk) {
            this.thunk = thunk;
        }

        @Override
        public A value() {
            return thunk.get();
        }

        @Override
        public Eval<A> memoize() {
            return new Later<>(thunk);
        }
    }

    /**
     * Construct an eager Eval[A] value (i.e. Now[A]).
     */
    public static <A> Eval<A> now(A a) {
        return new Now<>(a);
    }

    /**
     * Construct a lazy Eval[A] value with caching (i.e. Later[A]).
     */
    public static <A> Eval<A> later(Supplier<A> a) {
        return new Later<>(a);
    }

    /**
     * Construct a lazy Eval[A] value without caching (i.e. Always[A]).
     */
    public static <A> Eval<A> always(Supplier<A> a) {
        return new Always<>(a);
    }

    /**
     * Defer a computation which produces an Eval[A] value.
     * <p>
     * This is useful when you want to delay execution of an expression
     * which produces an Eval[A] value. Like .flatMap, it is stack-safe.
     */
    public static <A> Eval<A> defer(Supplier<Eval<A>> a) {
        return new Defer<>(a);
    }

    /**
     * Static Eval instance for common value `Unit`.
     * <p>
     * This can be useful in cases where the same value may be needed
     * many times.
     */
    public static final Eval<Unit> unit = now(Unit.unit());

    /**
     * Static Eval instance for common value `true`.
     * <p>
     * This can be useful in cases where the same value may be needed
     * many times.
     */
    public static final Eval<Boolean> True = now(true);

    /**
     * Static Eval instance for common value `false`.
     * <p>
     * This can be useful in cases where the same value may be needed
     * many times.
     */
    public static final Eval<Boolean> False = now(false);

    /**
     * Static Eval instance for common value `0`.
     * <p>
     * This can be useful in cases where the same value may be needed
     * many times.
     */
    public static final Eval<Integer> Zero = now(0);
    /**
     * Static Eval instance for common value `0`.
     * <p>
     * This can be useful in cases where the same value may be needed
     * many times.
     */
    public static final Eval<Integer> One = now(1);

    /**
     * Defer is a type of Eval[A] that is used to defer computations
     * which produce Eval[A].
     * <p>
     * Users should not instantiate Defer instances themselves. Instead,
     * they will be automatically created when needed.
     */
    public static class Defer<A> extends Eval<A> {
        private final Supplier<Eval<A>> thunk;

        private Defer(Supplier<Eval<A>> thunk) {
            this.thunk = thunk;
        }

        @Override
        public Eval<A> memoize() {
            return new Memoize<>(this);
        }

        @Override
        public A value() {
            return evaluate(this);
        }

        @Override
        public <B> Eval<B> flatMap(Function1<A, Eval<B>> f) {
            return new FlatMap<B, A>() {

                @Override
                public Supplier<Eval<A>> start() {
                    return thunk;
                }

                @Override
                public Eval<B> run(A a) {
                    return f.apply(a);
                }
            };
        }
    }

    /**
     * Advance until we find a non-deferred Eval node.
     * <p>
     * Often we may have deep chains of Defer nodes; the goal here is to
     * advance through those to find the underlying "work" (in the case
     * of FlatMap nodes) or "value" (in the case of Now, Later, or
     * Always nodes).
     */
    private static <A> Eval<A> advance(final Eval<A> fa) {
        if (fa instanceof Defer) {
            return advance(((Defer<A>) fa).thunk.get());
        } else if (fa instanceof FlatMap) {
            var compute = (FlatMap<A, Object>) fa;
            return new FlatMap<>() {

                @Override
                public Supplier<Eval<Object>> start() {
                    return compute.start();
                }

                @Override
                public Eval<A> run(Object s) {
                    return advance(compute.run(s));
                }
            };
        } else {
            return fa;
        }
    }

    private static abstract class FlatMap<A, Start> extends Eval<A> {

        public abstract Supplier<Eval<Start>> start();

        public abstract Eval<A> run(Start start);

        @Override
        public final Eval<A> memoize() {
            return new Memoize<>(this);
        }

        @Override
        public final A value() {
            return evaluate(this);
        }

        @Override
        public <B> Eval<B> flatMap(Function1<A, Eval<B>> f) {
            final var self = this;
            return new FlatMap<B, Start>() {

                @Override
                public Supplier<Eval<Start>> start() {
                    return self.start();
                }

                @Override
                public Eval<B> run(final Start start) {
                    return new FlatMap<B, A>() {

                        @Override
                        public Supplier<Eval<A>> start() {
                            return () -> self.run(start);
                        }

                        @Override
                        public Eval<B> run(A a) {
                            return f.apply(a);
                        }
                    };
                }
            };
        }
    }

    private static final class Memoize<A> extends Eval<A> {
        private Eval<A> eval;
        private Maybe<A> result = Maybe.none();

        private Memoize(Eval<A> eval) {
            this.eval = eval;
        }

        @Override
        public A value() {
            if (result.isEmpty()) {
                result = Maybe.some(evaluate(this));
                eval = null;
            }
            return result.get();
        }

        @Override
        public final Eval<A> memoize() {
            return this;
        }
    }

    @SuppressWarnings("unchecked")
    private static <A> A evaluate(Eval<A> e) {
        Eval curr = e;
        List<Function1<Object, Eval>> fs = List.nil();
        do {
            if (curr instanceof FlatMap) {
                var c = (FlatMap<Object, Object>) curr;
                var xx = c.start().get();
                if (xx instanceof FlatMap) {
                    var cc = (FlatMap<Object, Object>) curr;
                    curr = cc.start().get();
                    fs = fs.prepend(c::run).prepend(cc::run);
                } else if (xx instanceof Memoize) {
                    var mm = (Memoize) xx;
                    if (mm.result.isDefined()) {
                        var a = mm.result.get();
                        curr = new Now<>(a);
                    } else {
                        fs = fs.prepend(c::run).prepend(aa -> {
                            mm.result = Maybe.some(aa);
                            return new Now<>(aa);
                        });
                    }
                } else {
                    curr = c.run(xx.value());
                }
            } else if (curr instanceof Defer) {
                var call = (Defer) curr;
                curr = advance(call);
            } else if (curr instanceof Memoize) {
                var m = (Memoize) curr;
                if (m.result.isDefined()) {
                    var a = m.result.get();
                    if (fs.nonEmpty()) {
                        curr = fs.head().apply(a);
                        fs = fs.tail();
                    } else {
                        return (A) a;
                    }
                } else {
                    curr = m.eval;
                    fs = fs.prepend(a -> {
                        m.result = Maybe.some(a);
                        return new Now<>(a);
                    });
                }
            } else {
                if (fs.nonEmpty()) {
                    curr = fs.head().apply(curr.value());
                    fs = fs.tail();
                } else {
                    return (A) curr.value();
                }
            }
        } while (true);
    }

    public static <A> Semigroup<Eval<A>> semigroup(final Semigroup<A> semigroup) {
        return (EvalSemigroup<A>) () -> semigroup;
    }

    public static <A> Monoid<Eval<A>> monoid(final Monoid<A> monoid) {
        return (EvalMonoid<A>) () -> monoid;
    }

    public static <A> Group<Eval<A>> group(final Group<A> group) {
        return (EvalGroup<A>) () -> group;
    }
}

interface EvalSemigroup<A> extends Semigroup<Eval<A>> {
    Semigroup<A> algebra();

    @Override
    default Eval<A> combine(Eval<A> lx, Eval<A> ly) {
        return lx.flatMap(x -> ly.map(y -> algebra().combine(x, y)));
    }
}

interface EvalMonoid<A> extends Monoid<Eval<A>>, EvalSemigroup<A> {
    @Override
    Monoid<A> algebra();

    @Override
    default Eval<A> empty() {
        return Eval.later(() -> algebra().empty());
    }
}

interface EvalGroup<A> extends Group<Eval<A>>, EvalMonoid<A> {
    @Override
    Group<A> algebra();

    @Override
    default Eval<A> inverse(Eval<A> lx) {
        return lx.map(a -> algebra().inverse(a));
    }

    @Override
    default Eval<A> remove(Eval<A> lx, Eval<A> ly) {
        return lx.flatMap(x -> ly.map(y -> algebra().remove(x, y)));
    }
}