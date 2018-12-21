package io.mitallast.io;

import io.mitallast.categories.Applicative;
import io.mitallast.higher.Higher;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;
import io.mitallast.product.Tuple2;

import java.util.function.BiFunction;

public interface Fiber<F extends Higher, A> extends Higher<Fiber<F, ?>, A> {
    /**
     * Triggers the cancellation of the fiber.
     * <p>
     * Returns a new task that will trigger the cancellation upon
     * evaluation. Depending on the implementation, this task might
     * await for all registered finalizers to finish, but this behavior
     * is implementation dependent.
     * <p>
     * Note that if the background process that's evaluating the result
     * of the underlying fiber is already complete, then there's nothing
     * to cancel.
     */
    Higher<F, Unit> cancel();

    /**
     * Returns a new task that will await for the completion of the
     * underlying fiber, (asynchronously) blocking the current run-loop
     * until that result is available.
     */
    Higher<F, A> join();

    static <F extends Higher, A> Fiber<F, A> apply(Higher<F, A> join, Higher<F, Unit> cancel) {
        return new FiberTuple<>(join, cancel);
    }
}

class FiberTuple<F extends Higher, A> implements Fiber<F, A> {
    private final Higher<F, A> join;
    private final Higher<F, Unit> cancel;

    FiberTuple(Higher<F, A> join, Higher<F, Unit> cancel) {
        this.join = join;
        this.cancel = cancel;
    }

    @Override
    public Higher<F, Unit> cancel() {
        return cancel;
    }

    @Override
    public Higher<F, A> join() {
        return join;
    }

    public static <F extends Higher> Applicative<Fiber<F, ?>> applicative(Concurrent<F> a) {
        return new FiberApplicative<>(a);
    }
}

class FiberApplicative<F extends Higher> implements Applicative<Fiber<F, ?>> {
    private final Concurrent<F> F;

    FiberApplicative(Concurrent<F> f) {
        F = f;
    }

    @Override
    public <A> Higher<Fiber<F, ?>, A> pure(A a) {
        return Fiber.apply(F.pure(a), F.unit());
    }

    @Override
    public <A, B> Higher<Fiber<F, ?>, B> ap(Higher<Fiber<F, ?>, Function1<A, B>> ff, Higher<Fiber<F, ?>, A> fa) {
        return map2(ff, fa, Function1::apply);
    }

    @Override
    public <A, B, Z> Higher<Fiber<F, ?>, Z> map2(Higher<Fiber<F, ?>, A> fa, Higher<Fiber<F, ?>, B> fb, BiFunction<A, B, Z> f) {
        var ffa = (Fiber<F, A>) fa;
        var ffb = (Fiber<F, B>) fb;
        var fa2 = F.guaranteeCase(ffa.join(), e -> {
            if (e instanceof ExitCase.Error) {
                return ffb.cancel();
            } else {
                return F.unit();
            }
        });
        var fb2 = F.guaranteeCase(ffb.join(), e -> {
            if (e instanceof ExitCase.Error) {
                return ffa.cancel();
            } else {
                return F.unit();
            }
        });
        return Fiber.apply(
            F.flatMap(F.racePair(fa2, fb2), e -> e.fold(
                tuple -> F.map(F.product(F.pure(tuple.t1()), tuple.t2().join()), t -> f.apply(t.t1(), t.t2())),
                tuple -> F.map(F.product(tuple.t1().join(), F.pure(tuple.t2())), t -> f.apply(t.t1(), t.t2()))
            )),
            F.map2(ffa.cancel(), ffb.cancel(), (u1, u2) -> Unit.unit())
        );
    }

    @Override
    public <A, B> Higher<Fiber<F, ?>, Tuple2<A, B>> product(Higher<Fiber<F, ?>, A> fa, Higher<Fiber<F, ?>, B> fb) {
        return map2(fa, fb, Tuple2::new);
    }

    @Override
    public <A, B> Higher<Fiber<F, ?>, B> map(Higher<Fiber<F, ?>, A> fa, Function1<A, B> f) {
        var ffa = (Fiber<F, A>) fa;
        return Fiber.apply(F.map(ffa.join(), f), ffa.cancel());
    }

    @Override
    public Higher<Fiber<F, ?>, Unit> unit() {
        return Fiber.apply(F.unit(), F.unit());
    }
}
