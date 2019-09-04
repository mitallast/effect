package io.mitallast.stream;

import io.mitallast.categories.Functor;
import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.io.Async;
import io.mitallast.io.ExitCase;
import io.mitallast.io.Sync;
import io.mitallast.io.Timer;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Function2;
import io.mitallast.lambda.Function3;
import io.mitallast.lambda.Supplier;
import io.mitallast.list.List;
import io.mitallast.maybe.Maybe;
import io.mitallast.product.Tuple2;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.function.LongFunction;
import java.util.function.Predicate;

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

    public Stream<F, O> cons(Chunk<O> c) {
        if (c.isEmpty()) return this;
        else return Stream.<F, O>chunk(c).append(() -> this);
    }

    public Stream<F, Chunk<O>> chunks() {
        return invariantOps().repeatPull(tp -> tp.<Chunk<O>>uncons().flatMap(opt -> opt.fold(
            () -> Pull.pure(Maybe.none()),
            t -> {
                var hd = t.t1();
                var tl = t.t2();
                return Pull.<F, Chunk<O>>output1(hd).as(Maybe.some(tl));
            }
        )));
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

    public Stream<F, Maybe<O>> last() {
        return pull().<Maybe<O>>last().flatMap(Pull::output1).stream();
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

    public Stream<F, O> repeat() {
        return append(this::repeat);
    }

    public <O2> Stream<F, O2> rethrow() {
        @SuppressWarnings("unchecked")
        var cast = (Stream<F, Either<Throwable, O2>>) this;
        return cast.chunks().flatMap(new Function1<Chunk<Either<Throwable, O2>>, Stream<F, O2>>() {
            @Override
            public Stream<F, O2> apply(final Chunk<Either<Throwable, O2>> c) {
                var firstError = c.find(Either::isLeft).map(e -> e.left().get());
                return firstError.fold(
                    () -> Stream.chunk(c.filter(Either::isRight).map(e -> e.right().get())),
                    Stream::raiseError
                );
            }
        });
    }

    public Stream<F, O> take(long n) {
        return pull().take(n).stream();
    }

    public Stream<F, O> takeRight(int n) {
        return pull()
            .takeRight(n)
            .flatMap(cq ->
                cq.chunks.foldLeft(
                    Pull.done(),
                    (acc, c) -> acc.flatMap(u -> Pull.output(c))
                )
            )
            .stream();
    }

    public Stream<F, O> takeThrough(Predicate<O> p) {
        return this.pull().takeThrough(p).stream();
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

    @SafeVarargs
    public static <F extends Higher, O> Stream<F, O> apply(O... os) {
        return emits(Arrays.asList(os));
    }

    public static <F extends Higher, O> Stream<F, Either<Throwable, O>> attemptEval(Higher<F, O> fo) {
        return Stream.fromFreeC(Pull.<F, Either<Throwable, O>, O>attemptEval(fo).flatMap(Pull::output1).get());
    }

    public static <F extends Higher> Stream<F, Duration> awakeDelay(Duration d, Timer<F> timer, Functor<F> F) {
        return Stream.eval(timer.clock().monotonic(TimeUnit.NANOSECONDS))
            .flatMap(start ->
                Stream.fixedDelay(d, timer).flatMap(u ->
                    Stream.eval(F.map(
                        timer.clock().monotonic(TimeUnit.NANOSECONDS),
                        now -> Duration.ofNanos(now - start)
                    ))));
    }

    public static <F extends Higher> Stream<F, Duration> awakeEvery(Duration d, Timer<F> timer, Functor<F> F) {
        return Stream.eval(timer.clock().monotonic(TimeUnit.NANOSECONDS))
            .flatMap(start ->
                Stream.fixedRate(d, timer).flatMap(u ->
                    Stream.eval(F.map(
                        timer.clock().monotonic(TimeUnit.NANOSECONDS),
                        now -> Duration.ofNanos(now - start)
                    ))));
    }

    public static <F extends Higher, R> Stream<F, R> bracket(
        final Higher<F, R> acquire,
        final Function1<R, Higher<F, Unit>> release
    ) {
        return bracketCase(acquire, (r, i) -> release.apply(r));
    }

    public static <F extends Higher, R> Stream<F, R> bracketCase(
        final Higher<F, R> acquire,
        final Function2<R, ExitCase<Throwable>, Higher<F, Unit>> release
    ) {
        return fromFreeC(Algebra.<F, R, R>acquire(acquire, release).flatMap(t -> {
            return Stream.<F, R>emit(t.t1()).get();
        }));
    }

    public static <F extends Higher, R> Stream<F, Tuple2<Stream<F, Unit>, R>> bracketCancellable(
        final Higher<F, R> acquire,
        final Function1<R, Higher<F, Unit>> release
    ) {
        return bracketCaseCancellable(acquire, (r, e) -> release.apply(r));
    }

    public static <F extends Higher, R> Stream<F, Tuple2<Stream<F, Unit>, R>> bracketCaseCancellable(
        final Higher<F, R> acquire,
        final Function2<R, ExitCase<Throwable>, Higher<F, Unit>> release
    ) {
        return bracketWithResource(acquire, release).map(t -> {
            var res = t.t1();
            var r = t.t2();
            var stream = Stream.eval(res.release(ExitCase.canceled())).flatMap(e -> e.fold(
                lt -> Stream.fromFreeC(Algebra.raiseError(lt)),
                Stream::emit
            ));
            return new Tuple2<>(stream, r);
        });
    }

    private static <F extends Higher, R> Stream<F, Tuple2<io.mitallast.stream.Resource<F>, R>> bracketWithResource(
        final Higher<F, R> acquire,
        final Function2<R, ExitCase<Throwable>, Higher<F, Unit>> release
    ) {
        return fromFreeC(Algebra.<F, Tuple2<io.mitallast.stream.Resource<F>, R>, R>acquire(acquire, release).flatMap(t -> {
            var r = t.t1();
            var res = t.t2();
            return Stream.<F, R>emit(r).map(o -> new Tuple2<>(res, o)).get();
        }));
    }

    public static <F extends Higher, O> Stream<F, O> chunk(Chunk<O> os) {
        return Stream.fromFreeC(Algebra.output(os));
    }

    public static <F extends Higher, O> Stream<F, O> constant(O o) {
        return constant(o, 256);
    }

    public static <F extends Higher, O> Stream<F, O> constant(O o, int chunkSize) {
        return Stream.<F, O>chunk(Chunk.fill(chunkSize, o)).repeat();
    }

    public static <F extends Higher> Stream<F, Duration> duration(Sync<F> F) {
        return Stream.eval(F.delay(System::nanoTime)).flatMap(t0 ->
            Stream.repeatEval(F.delay(() -> Duration.ofNanos(System.nanoTime() - t0)))
        );
    }

    public static <F extends Higher, O> Stream<F, O> emit(O value) {
        return fromFreeC(Algebra.output1(value));
    }

    public static <F extends Higher, O> Stream<F, O> emits(Collection<O> os) {
        if (os.isEmpty()) return empty();
        else if (os.size() == 1) return emit(os.iterator().next());
        else return fromFreeC(Algebra.output(Chunk.seq(os)));
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

    public static <F extends Higher> Stream<F, Boolean> every(Duration d, Timer<F> timer) {
        var go = new LongFunction<Stream<F, Boolean>>() {
            @Override
            public Stream<F, Boolean> apply(final long lastSpikeNanos) {
                return Stream.eval(timer.clock().monotonic(TimeUnit.NANOSECONDS)).flatMap(now -> {
                    if ((now - lastSpikeNanos) > d.toNanos()) {
                        return Stream.<F, Boolean>emit(true).append(() -> this.apply(now));
                    } else {
                        return Stream.<F, Boolean>emit(false).append(() -> this.apply(lastSpikeNanos));
                    }
                });
            }
        };
        return go.apply(0);
    }

    public static <F extends Higher> Stream<F, Unit> fixedDelay(Duration d, Timer<F> timer) {
        return sleep(d, timer).repeat();
    }

    public static <F extends Higher> Stream<F, Unit> fixedRate(Duration d, Timer<F> timer) {
        Supplier<Stream<F, Long>> now = () -> Stream.eval(timer.clock().monotonic(TimeUnit.NANOSECONDS));
        var loop = new Function1<Long, Stream<F, Unit>>() {
            @Override
            public Stream<F, Unit> apply(final Long started) {
                return now.get().flatMap(finished -> {
                    var elapsed = finished - started;
                    return Stream.sleep(d.minusNanos(elapsed), timer)
                        .append(() -> now.get()
                            .flatMap(st -> Stream.<F, Unit>emit(Unit.unit())
                                .append(() -> this.apply(st))));
                });
            }
        };
        return now.get().flatMap(loop);
    }

    public static <F extends Higher, A> Stream<F, A> fromIterator(Iterator<A> iterator, Sync<F> F) {
        return Stream.unfoldEval(iterator, it -> F.flatMap(
            F.delay(it::hasNext),
            has -> {
                if (has) {
                    return F.map(
                        F.delay(it::next),
                        a -> Maybe.some(new Tuple2<>(a, it))
                    );
                } else {
                    return F.pure(Maybe.none());
                }
            }
        ));
    }

    public static <F extends Higher, A> Stream<F, A> force(Higher<F, Stream<F, A>> f) {
        return eval(f).flatMap(s -> s);
    }

    public static <F extends Higher, A> Stream<F, A> iterate(A start, Function1<A, A> f) {
        return Stream.<F, A>emit(start).append(() -> iterate(f.apply(start), f));
    }

    public static <F extends Higher, A> Stream<F, A> iterateEval(A start, Function1<A, Higher<F, A>> f) {
        return Stream.<F, A>emit(start).append(() -> eval(f.apply(start)).flatMap(a -> iterateEval(a, f)));
    }

    public static <F extends Higher> Stream<F, Unit> never(Async<F> F) {
        return Stream.eval(F.never());
    }

    public static <F extends Higher, O> Stream<F, O> raiseError(Throwable e) {
        return fromFreeC(Algebra.raiseError(e));
    }

    public static <F extends Higher> Stream<F, Integer> random(Sync<F> F) {
        return Stream.eval(F.delay(Random::new)).flatMap(random -> {
            var loop = new Supplier<Stream<F, Integer>>() {
                @Override
                public Stream<F, Integer> get() {
                    return Stream.<F, Integer>emit(random.nextInt()).append(this);
                }
            };
            return loop.get();
        });
    }

    public static <F extends Higher> Stream<F, Integer> randomSeeded(long seed) {
        return Stream.suspend(() -> {
            var r = new Random(seed);
            var loop = new Supplier<Stream<F, Integer>>() {
                @Override
                public Stream<F, Integer> get() {
                    return Stream.<F, Integer>emit(r.nextInt()).append(this);
                }
            };
            return loop.get();
        });
    }

    public static <F extends Higher> Stream<F, Integer> range(int start, int stopExclusive) {
        return range(start, stopExclusive, 1);
    }

    public static <F extends Higher> Stream<F, Integer> range(int start, int stopExclusive, int by) {
        return unfold(start, i -> {
            if ((by > 0 && i < stopExclusive && start < stopExclusive) ||
                (by < 0 && i > stopExclusive && start > stopExclusive)) {
                return Maybe.some(new Tuple2<>(i, i + by));
            } else {
                return Maybe.none();
            }
        });
    }

    public static <F extends Higher> Stream<F, Tuple2<Integer, Integer>> ranges(int start, int stopExclusive, int size) {
        if (size <= 0) throw new IllegalArgumentException("size must be greater than 0");
        return unfold(start, lower -> {
            if (lower < stopExclusive) {
                return Maybe.some(new Tuple2<>(new Tuple2<>(lower, Math.min(stopExclusive, lower + size)), lower + size));
            } else {
                return Maybe.none();
            }
        });
    }

    public static <F extends Higher, O> Stream<F, O> repeatEval(Higher<F, O> fo) {
        return eval(fo).repeat();
    }

    public static <F extends Higher, O> Stream<F, O> retry(
        final Higher<F, O> fo,
        final Duration delay,
        final Function1<Duration, Duration> nextDelay,
        final int maxAttempts,
        final Timer<F> timer
    ) {
        return retry(fo, delay, nextDelay, maxAttempts, e -> !(e instanceof Error), timer);
    }

    public static <F extends Higher, O> Stream<F, O> retry(
        final Higher<F, O> fo,
        final Duration delay,
        final Function1<Duration, Duration> nextDelay,
        final int maxAttempts,
        final Predicate<Throwable> retriable,
        final Timer<F> timer
    ) {
        assert maxAttempts > 0 : "max attempts should be greater than 0";
        var delays = Stream.<F, Duration, Duration>unfold(delay, d -> Maybe.some(new Tuple2<>(d, nextDelay.apply(d))));
        return Stream.eval(fo)
            .attempts(delays, timer)
            .take(maxAttempts)
            .takeThrough(e -> e.fold(
                retriable::test,
                u -> false
            ))
            .last()
            .map(Maybe::get)
            .rethrow();
    }

    public static <F extends Higher, O> Stream<F, O> suspend(Supplier<Stream<F, O>> s) {
        return fromFreeC(Algebra.suspend(() -> s.get().free));
    }

    public static <F extends Higher, S, O> Stream<F, O> unfold(S init, Function1<S, Maybe<Tuple2<O, S>>> f) {
        var loop = new Function1<S, Stream<F, O>>() {
            @Override
            public Stream<F, O> apply(final S s) {
                return f.apply(s).fold(
                    Stream::empty,
                    t -> Stream.<F, O>emit(t.t1()).append(() -> this.apply(t.t2()))
                );
            }
        };
        return suspend(() -> loop.apply(init));
    }

    public static <F extends Higher, S, O> Stream<F, O> unfoldChunk(S init, Function1<S, Maybe<Tuple2<Chunk<O>, S>>> f) {
        return Stream.<F, S, Chunk<O>>unfold(init, f).flatMap(Stream::chunk);
    }

    public static <F extends Higher, S, O> Stream<F, O> unfoldEval(
        final S init,
        final Function1<S, Higher<F, Maybe<Tuple2<O, S>>>> f
    ) {
        var loop = new Function1<S, Stream<F, O>>() {
            @Override
            public Stream<F, O> apply(final S s) {
                return eval(f.apply(s)).flatMap(m -> m.fold(
                    Stream::empty,
                    t -> Stream.<F, O>emit(t.t1()).append(() -> this.apply(t.t2()))
                ));
            }
        };
        return suspend(() -> loop.apply(init));
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

        public Pull<F, O, Maybe<Tuple2<Chunk<O>, Stream<F, O>>>> unconsN(int n, boolean allowFewer) {
            if (n <= 0) return Pull.pure(Maybe.some(new Tuple2<>(Chunk.empty(), self())));
            else {
                var go = new Function3<
                    List<Chunk<O>>,
                    Integer,
                    Stream<F, O>,
                    Pull<F, O, Maybe<Tuple2<Chunk<O>, Stream<F, O>>>>>() {

                    @Override
                    public Pull<F, O, Maybe<Tuple2<Chunk<O>, Stream<F, O>>>> apply(
                        final List<Chunk<O>> acc,
                        final Integer n,
                        final Stream<F, O> s) {
                        var go = this;

                        return s.pull()
                            .<O>uncons()
                            .flatMap(opt -> opt.fold(
                                () -> {
                                    if (allowFewer && acc.nonEmpty()) {
                                        return Pull.pure(Maybe.some(new Tuple2<>(Chunk.concat(acc.reverse()), Stream.empty())));
                                    } else {
                                        return Pull.pure(Maybe.none());
                                    }
                                },
                                t -> {
                                    var hd = t.t1();
                                    var tl = t.t2();
                                    if (hd.size() < n) {
                                        return go.apply(acc.prepend(hd), n - hd.size(), tl);
                                    } else if (hd.size() == n) {
                                        return Pull.pure(Maybe.some(new Tuple2<>(
                                            Chunk.concat(acc.prepend(hd).reverse()),
                                            tl
                                        )));
                                    } else {
                                        var fx = hd.splitAt(n);
                                        return Pull.pure(Maybe.some(new Tuple2<>(
                                            Chunk.concat(acc.prepend(fx.t1()).reverse()),
                                            tl.cons(fx.t2())
                                        )));
                                    }
                                }
                            ));
                    }
                };
                return go.apply(List.nil(), n, self());
            }
        }

        public Pull<F, O, Maybe<Stream<F, O>>> take(final long n) {
            if (n <= 0) return Pull.pure(Maybe.none());
            else return this.<O>uncons().flatMap(opt -> opt.fold(
                () -> Pull.pure(Maybe.none()),
                t -> {
                    var hd = t.t1();
                    var tl = t.t2();
                    var m = hd.size();
                    if (m < n) return Pull.<F, O>output(hd).flatMap(unit -> tl.pull().take(n - m));
                    else if (m == n) return Pull.<F, O>output(hd).as(Maybe.some(tl));
                    else {
                        var s = hd.splitAt((int) n);
                        return Pull.<F, O>output(s.t1()).as(Maybe.some(tl.cons(s.t2())));
                    }
                }
            ));
        }

        public Pull<F, O, Chunk.CQueue<O>> takeRight(int n) {
            if (n <= 0) return Pull.pure(Chunk.CQueue.empty());
            else {
                var go = new Function2<Chunk.CQueue<O>, Stream<F, O>, Pull<F, O, Chunk.CQueue<O>>>() {

                    @Override
                    public Pull<F, O, Chunk.CQueue<O>> apply(Chunk.CQueue<O> acc, Stream<F, O> s) {
                        var go = this;
                        return s.pull().unconsN(n, true).flatMap(opt -> opt.fold(
                            () -> Pull.pure(acc),
                            t -> {
                                var hd = t.t1();
                                var tl = t.t2();
                                return go.apply(acc.drop(hd.size()).append(hd), tl);
                            }
                        ));
                    }
                };
                return go.apply(Chunk.CQueue.empty(), self());
            }
        }

        public Pull<F, O, Maybe<Stream<F, O>>> takeThrough(Predicate<O> p) {
            return takeWhile(p, true);
        }

        public Pull<F, O, Maybe<Stream<F, O>>> takeWhile(Predicate<O> p, boolean takeFailure) {
            return this.<O>uncons().flatMap(opt -> opt.fold(
                () -> Pull.pure(Maybe.none()),
                t -> {
                    var hd = t.t1();
                    var tl = t.t2();
                    return hd.indexWhere(o -> !p.test(o)).fold(
                        () -> Pull.<F, O>output(hd).flatMap(u -> tl.pull().takeWhile(p, takeFailure)),
                        idx -> {
                            var toTake = takeFailure ? idx + 1 : idx;
                            var fx = hd.splitAt(toTake);
                            return Pull.<F, O>output(fx.t1()).flatMap(u -> Pull.pure(Maybe.some(tl.cons(fx.t2()))));
                        }
                    );
                }
            ));
        }

        public Pull<F, O, Unit> echo() {
            return Pull.fromFreeC(free);
        }

        public <O2> Pull<F, O2, Maybe<O>> last() {
            var go = new Function2<Maybe<O>, Stream<F, O>, Pull<F, O2, Maybe<O>>>() {
                @Override
                public Pull<F, O2, Maybe<O>> apply(final Maybe<O> prev, final Stream<F, O> s) {
                    var go = this;
                    return s.pull().<O2>uncons().flatMap(opt -> opt.fold(
                        () -> Pull.pure(prev),
                        t -> {
                            var hd = t.t1();
                            var tl = t.t2();
                            return go.apply(hd.last().orElse(prev), tl);
                        }
                    ));
                }
            };
            return go.apply(Maybe.none(), self());
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
