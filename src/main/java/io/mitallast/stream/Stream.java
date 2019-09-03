package io.mitallast.stream;

import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.io.Sync;
import io.mitallast.io.Timer;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Function2;
import io.mitallast.lambda.Supplier;
import io.mitallast.list.List;
import io.mitallast.maybe.Maybe;
import io.mitallast.product.Tuple2;

import java.time.Duration;
import java.util.function.IntFunction;

public final class Stream<F extends Higher, O> {
    private final FreeC<Algebra<F, O, ?>, Unit> free;

    private Stream(FreeC<Algebra<F, O, ?>, Unit> free) {
        this.free = free;
    }

    FreeC<Algebra<F, O, ?>, Unit> get() {
        return free;
    }

    public Stream<F, O> append(Supplier<Stream<F, O>> s2) {
        return Stream.fromFreeC(get().transformWith(result -> {
            if (result instanceof FreeC.Result.Pure) {
                var pure = (FreeC.Result.Pure<F, O>) result;
                return s2.get().get();
            } else {
                return result.asFreeC();
            }
        }));
    }

    public <O2> Stream<F, O2> as(O2 o2) {
        return map(o -> o2);
    }

    public Stream<F, Either<Throwable, O>> attempt() {
        return this.<Either<Throwable, O>>map(Either::right).handleErrorWith(e -> Stream.emit(Either.left(e)));
    }

    public Stream<F, Either<Throwable, O>> attempts(Stream<F, Duration> delays, Timer<F> timer) {
        return attempt().append(() ->
            delays.flatMap(delay ->
                Stream.sleep(delay, timer).<Either<Throwable, O>>drain().append(this::attempt)));
    }

    public <O2> Stream<F, O2> evalMap(Function1<O, Higher<F, O2>> f) {
        return flatMap(o -> Stream.eval(f.apply(o)));
    }

    public <O2> Stream<F, O2> flatMap(Function1<O, Stream<F, O2>> f) {
        return Stream.fromFreeC(Algebra.<F, O2, O>uncons(get()).flatMap(
            opt -> opt.fold(
                () -> Stream.<F, O2>empty().get(),
                t -> {
                    final Chunk<O> hd = t.t1();
                    final FreeC<Algebra<F, O, ?>, Unit> tl = t.t2();
                    if (tl instanceof FreeC.Result.Pure && hd.size() == 1) {
                        return f.apply(hd.apply(0)).get();
                    } else {
                        var go = new IntFunction<FreeC<Algebra<F, O2, ?>, Unit>>() {
                            @Override
                            public FreeC<Algebra<F, O2, ?>, Unit> apply(final int idx) {
                                if (idx == hd.size()) {
                                    return Stream.fromFreeC(tl).flatMap(f).get();
                                } else {
                                    return f.apply(hd.apply(idx))
                                        .get()
                                        .transformWith(
                                            r -> {
                                                if (r instanceof FreeC.Result.Pure) {
                                                    return this.apply(idx + 1);
                                                } else if (r instanceof FreeC.Result.Fail) {
                                                    var err = ((FreeC.Result.Fail) r).error;
                                                    return Algebra.raiseError(err);
                                                } else if (r instanceof FreeC.Result.Interrupted) {
                                                    @SuppressWarnings("unckecked")
                                                    var interrupted = (FreeC.Result.Interrupted<F, O, ?>) r;
                                                    var scope = (Token) interrupted.context;
                                                    var err = interrupted.deferredError;
                                                    return Stream.fromFreeC(Algebra.interruptBoundary(tl, scope, err)).flatMap(f).get();
                                                } else throw new IllegalArgumentException();
                                            }
                                        );
                                }
                            }
                        };
                        return go.apply(0);
                    }
                }
            )
        ));
    }

    public Stream<F, O> handleErrorWith(Function1<Throwable, Stream<F, O>> h) {
        return Stream.fromFreeC(Algebra.scope(get()).handleErrorWith(e -> h.apply(e).get()));
    }

    public <O2> Stream<F, O2> map(Function1<O, O2> f) {
        return new ToPull<>(free).echo().mapOutput(f).streamNoScope();
    }

    public <O2> Stream<F, O2> mapChunks(Function1<Chunk<O>, Chunk<O2>> f) {
        return invariantOps().repeatPull(p -> p.<O2>uncons().flatMap(m -> m.fold(
            () -> Pull.pure(Maybe.none()),
            t -> {
                var hd = t.t1();
                var tl = t.t2();
                return Pull.<F, O2>output(f.apply(hd)).as(Maybe.some(tl));
            }
        )));
    }

    public <O2> Stream<F, O2> drain() {
        return mapChunks(c -> Chunk.empty());
    }

    private ToPull<F, O> pull() {
        return new ToPull<>(free);
    }

    private InvariantOps<F, O> invariantOps() {
        return new InvariantOps<>(free);
    }

    public <G extends Higher> CompileOps<F, G, O> compile(Stream.Compiler<F, G> compiler) {
        return new Stream.CompileOps<>(free, compiler);
    }

    // ------------- static

    static <F extends Higher, O> Stream<F, O> fromFreeC(FreeC<Algebra<F, O, ?>, Unit> free) {
        return new Stream<>(free);
    }

    public static <F extends Higher, O> Stream<F, O> emit(O value) {
        return fromFreeC(Algebra.output1(value));
    }

    public static <F extends Higher, O> Stream<F, O> empty() {
        return fromFreeC(Algebra.pure(Unit.unit()));
    }

    public static <F extends Higher, O> Stream<F, O> eval(Higher<F, O> fo) {
        return Stream.fromFreeC(Algebra.<F, O, O>eval(fo).flatMap(Algebra::output1));
    }

    public static <F extends Higher> Stream<F, Unit> sleep(Duration d, Timer<F> timer) {
        return eval(timer.sleep(d));
    }

    final static class InvariantOps<F extends Higher, O> {
        private final FreeC<Algebra<F, O, ?>, Unit> free;

        private InvariantOps(final FreeC<Algebra<F, O, ?>, Unit> free) {
            this.free = free;
        }

        private Stream<F, O> self() {
            return fromFreeC(free);
        }

        @SuppressWarnings("unchecked")
        public <F2 extends F> Stream<F2, O> covary() {
            return fromFreeC((FreeC<Algebra<F2, O, ?>, Unit>) (FreeC) free);
        }

        public ToPull<F, O> pull() {
            return new ToPull<>(free);
        }

        public <O2> Stream<F, O2> repeatPull(Function1<ToPull<F, O>, Pull<F, O2, Maybe<Stream<F, O>>>> using) {
            return Pull.loop(using.andThen(p -> p.map(m -> m.map(s -> s.pull())))).apply(pull()).stream();
        }
    }

    final static class ToPull<F extends Higher, O> {
        private final FreeC<Algebra<F, O, ?>, Unit> free;

        private ToPull(final FreeC<Algebra<F, O, ?>, Unit> free) {
            this.free = free;
        }

        private Stream<F, O> self() {
            return fromFreeC(free);
        }

        public <O2> Pull<F, O2, Maybe<Tuple2<Chunk<O>, Stream<F, O>>>> uncons() {
            return Pull.fromFreeC(Algebra.<F, O2, O>uncons(free).map(m -> m.map(t -> {
                var hd = t.t1();
                var tl = t.t2();
                return new Tuple2<>(hd, Stream.fromFreeC(tl));
            })));
        }

        public Pull<F, O, Unit> echo() {
            return Pull.fromFreeC(free);
        }
    }

    public interface Compiler<F extends Higher, G extends Higher> {
        <O, B, C> Higher<G, C> apply(
            Stream<F, O> s,
            Supplier<B> init,
            Function2<B, Chunk<O>, B> fold,
            Function1<B, C> finalize
        );

        private static <F extends Higher, O, B> Higher<F, B> compile(
            FreeC<Algebra<F, O, ?>, Unit> stream,
            B init,
            Function2<B, Chunk<O>, B> f,
            Sync<F> F
        ) {
            return F.bracketCase(
                CompileScope.newRoot(F),
                scope -> Algebra.compile(stream, scope, init, f, F),
                (scope, ec) -> F.rethrow(scope.close(ec))
            );
        }

        static <F extends Higher> Compiler<F, F> sync(Sync<F> F) {
            return new Compiler<F, F>() {
                @Override
                public <O, B, C> Higher<F, C> apply(final Stream<F, O> s,
                                                    final Supplier<B> init,
                                                    final Function2<B, Chunk<O>, B> foldChunk,
                                                    final Function1<B, C> finalize) {
                    return F.flatMap(
                        F.delay(init),
                        i -> F.map(Compiler.compile(s.get(), i, foldChunk, F), finalize)
                    );
                }
            };
        }
    }

    public final static class CompileOps<F extends Higher, G extends Higher, O> {
        private final FreeC<Algebra<F, O, ?>, Unit> free;
        private final Compiler<F, G> compiler;

        private CompileOps(final FreeC<Algebra<F, O, ?>, Unit> free, final Compiler<F, G> compiler) {
            this.free = free;
            this.compiler = compiler;
        }

        private Stream<F, O> self() {
            return Stream.fromFreeC(free);
        }

        public Higher<G, Unit> drain() {
            return foldChunks(Unit.unit(), (a, b) -> Unit.unit());
        }

        public <B> Higher<G, B> fold(B init, Function2<B, O, B> f) {
            return foldChunks(init, (acc, c) -> c.foldLeft(acc, f));
        }

        public <B> Higher<G, B> foldChunks(B init, Function2<B, Chunk<O>, B> f) {
            return compiler.apply(
                self(),
                () -> init,
                f,
                b -> b
            );
        }

        public Higher<G, Maybe<O>> last() {
            return foldChunks(Maybe.<O>none(), (acc, c) -> c.last().orElse(acc));
        }

        public Higher<G, Chunk<O>> toChunk() {
            return compiler.<O, List<Chunk<O>>, Chunk<O>>apply(
                self(),
                List::empty,
                List::prepend,
                Chunk::concat
            );
        }

        public Higher<G, List<O>> toList() {
            return fold(List.empty(), List::prepend);
        }
    }

    final static class StepLeg<F extends Higher, O> {
        final Chunk<O> head;
        final Token scopeId;
        final FreeC<Algebra<F, O, ?>, Unit> next;

        StepLeg(Chunk<O> head, Token scopeId, FreeC<Algebra<F, O, ?>, Unit> next) {
            this.head = head;
            this.scopeId = scopeId;
            this.next = next;
        }

        public Stream<F, O> stream() {
            return Pull.<F, O, StepLeg<F, O>>loop(leg -> Pull.<F, O>output(leg.head).flatMap(u -> leg.stepLeg()))
                .apply(this.setHead(Chunk.empty())).stream();
        }

        public StepLeg<F, O> setHead(Chunk<O> nextHead) {
            return new StepLeg<>(nextHead, scopeId, next);
        }

        public Pull<F, O, Maybe<StepLeg<F, O>>> stepLeg() {
            return Pull.fromFreeC(Algebra.stepLeg(this));
        }
    }
}
