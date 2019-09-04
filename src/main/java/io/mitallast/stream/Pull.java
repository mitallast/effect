package io.mitallast.stream;

import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;
import io.mitallast.maybe.Maybe;

final class Pull<F extends Higher, O, R> {
    private FreeC<Algebra<F, O, ?>, R> free;

    private Pull(FreeC<Algebra<F, O, ?>, R> free) {
        this.free = free;
    }

    FreeC<Algebra<F, O, ?>, R> get() {
        return free;
    }

    public <R2> Pull<F, O, R2> as(R2 r2) {
        return map(r -> r2);
    }

    public <R2> Pull<F, O, R2> flatMap(Function1<R, Pull<F, O, R2>> f) {
        return Pull.fromFreeC(free.flatMap(r -> f.apply(r).free));
    }

    public <R2> Pull<F, O, R2> map(Function1<R, R2> f) {
        return fromFreeC(free.map(f));
    }

    public <O2> Pull<F, O2, R> mapOutput(Function1<O, O2> f) {
        return Pull.fromFreeC(free.translate(Algebra.mapOutput(f)));
    }

    @SuppressWarnings("unchecked")
    public Stream<F, O> stream() {
        return Stream.fromFreeC((FreeC<Algebra<F, O, ?>, Unit>) free);
    }

    public Stream<F, O> streamNoScope() {
        return Stream.fromFreeC(free.map(i -> Unit.unit()));
    }

    // ---- static

    static <F extends Higher, O, R> Pull<F, O, Either<Throwable, R>> attemptEval(final Higher<F, R> fr) {
        return fromFreeC(Algebra.<F, O, R>eval(fr)
            .<Either<Throwable, R>>map(Either::right)
            .handleErrorWith(t -> Algebra.pure(Either.left(t))));
    }

    static <F extends Higher, O> Pull<F, O, Unit> done() {
        return fromFreeC(Algebra.pure(Unit.unit()));
    }

    static <F extends Higher, O, R> Pull<F, O, R> pure(R r) {
        return fromFreeC(Algebra.pure(r));
    }

    static <F extends Higher, O> Pull<F, O, Unit> output1(O o) {
        return fromFreeC(Algebra.output1(o));
    }

    static <F extends Higher, O> Pull<F, O, Unit> output(Chunk<O> os) {
        if (os.isEmpty()) {
            return Pull.done();
        } else {
            return fromFreeC(Algebra.output(os));
        }
    }

    static <F extends Higher, O, R> Pull<F, O, R> fromFreeC(
        FreeC<Algebra<F, O, ?>, R> free
    ) {
        return new Pull<>(free);
    }

    static <F extends Higher, O, R>
    Function1<R, Pull<F, O, Maybe<R>>>
    loop(Function1<R, Pull<F, O, Maybe<R>>> using) {
        return r -> using.apply(r).flatMap(m -> m.map(loop(using)).getOrElse(Pull.pure(Maybe.none())));
    }
}
