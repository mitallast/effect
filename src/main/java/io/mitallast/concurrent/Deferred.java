package io.mitallast.concurrent;

import io.mitallast.arrow.FunctionK;
import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.io.Async;
import io.mitallast.io.Concurrent;
import io.mitallast.io.Sync;
import io.mitallast.kernel.Unit;
import io.mitallast.maybe.Maybe;

import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public abstract class Deferred<F extends Higher, A> {
    public abstract Higher<F, A> get();

    public abstract Higher<F, Unit> complete(A a);

    public <G extends Higher> Deferred<G, A> mapK(FunctionK<F, G> f) {
        return new TransformedDeferred<>(this, f);
    }

    abstract static class TryableDeferred<F extends Higher, A> extends Deferred<F, A> {
        public abstract Higher<F, Maybe<A>> tryGet();
    }

    public static <F extends Higher, A> Higher<F, Deferred<F, A>> apply(Concurrent<F> F) {
        return F.delay(() -> unsafe(F));
    }

    public static <F extends Higher, A> Higher<F, TryableDeferred<F, A>> tryable(Concurrent<F> F) {
        return F.delay(() -> unsafeTryable(F));
    }

    public static <F extends Higher, A> Deferred<F, A> unsafe(Concurrent<F> F) {
        return unsafeTryable(F);
    }

    public static <F extends Higher, A> Higher<F, Deferred<F, A>> uncancelable(Async<F> F) {
        return F.delay(() -> unsafeUncancelable(F));
    }

    public static <F extends Higher, G extends Higher, A> Higher<F, Deferred<G, A>> in(Sync<F> F, Concurrent<G> G) {
        return F.delay(() -> unsafe(G));
    }

    public static <F extends Higher, G extends Higher, A> Higher<F, Deferred<G, A>> uncancelableIn(Sync<F> F, Async<G> G) {
        return F.delay(() -> unsafeUncancelable(G));
    }

    public static <F extends Higher, A> Higher<F, TryableDeferred<F, A>> tryableUncancelable(Async<F> F) {
        return F.delay(() -> unsafeTryableUncancelable(F));
    }

    public static <F extends Higher, A> TryableDeferred<F, A> unsafeUncancelable(Async<F> F) {
        return unsafeTryableUncancelable(F);
    }

    private static <F extends Higher, A> TryableDeferred<F, A> unsafeTryable(Concurrent<F> F) {
        return new ConcurrentDeferred<>(new AtomicReference<>(new State.Unset<>(new LinkedHashMap<>())), F);
    }

    private static <F extends Higher, A> TryableDeferred<F, A> unsafeTryableUncancelable(Async<F> F) {
        return new UncancelableDeferred<>(new CompletableFuture<>(), F);
    }

    private static final class Id {
    }

    private static abstract class State<A> {
        final static class Set<A> extends State<A> {
            final A a;

            Set(final A a) {
                this.a = a;
            }
        }

        final static class Unset<A> extends State<A> {
            final LinkedHashMap<Id, Consumer<A>> waiting;

            Unset(final LinkedHashMap<Id, Consumer<A>> waiting) {
                this.waiting = waiting;
            }
        }
    }

    private final static class ConcurrentDeferred<F extends Higher, A> extends TryableDeferred<F, A> {
        private final Concurrent<F> F;
        private final AtomicReference<State<A>> ref;

        ConcurrentDeferred(final AtomicReference<State<A>> ref, final Concurrent<F> F) {
            this.F = F;
            this.ref = ref;
        }

        @Override
        public Higher<F, A> get() {
            return F.suspend(() -> {
                var s = ref.get();
                if (s instanceof State.Set) {
                    return F.pure(((State.Set<A>) s).a);
                } else if (s instanceof State.Unset) {
                    return F.cancelable(cb -> {
                        final var id = unsafeRegister(cb);
                        return F.delay(() -> {
                            while (true) {
                                var ss = ref.get();
                                if (ss instanceof State.Set) {
                                    break;
                                } else if (ss instanceof State.Unset) {
                                    var waiting = ((State.Unset<A>) ss).waiting;
                                    var copy = new LinkedHashMap<>(waiting);
                                    copy.remove(id);
                                    var updated = new State.Unset<>(copy);
                                    if (ref.compareAndSet(s, updated)) {
                                        break;
                                    }
                                } else throw new IllegalArgumentException();
                            }
                            return Unit.unit();
                        });
                    });
                } else throw new IllegalArgumentException();
            });
        }

        @Override
        public Higher<F, Maybe<A>> tryGet() {
            return F.delay(() -> {
                var s = ref.get();
                if (s instanceof State.Set) {
                    return Maybe.some(((State.Set<A>) s).a);
                } else {
                    return Maybe.none();
                }
            });
        }

        private Id unsafeRegister(final Consumer<Either<Throwable, A>> cb) {
            var id = new Id();

            Maybe<A> register;
            while (true) {
                var s = ref.get();
                if (s instanceof State.Set) {
                    register = Maybe.some(((State.Set<A>) s).a);
                    break;
                } else if (s instanceof State.Unset) {
                    var waiting = ((State.Unset<A>) s).waiting;
                    var copy = new LinkedHashMap<>(waiting);
                    copy.put(id, a -> cb.accept(Either.right(a)));
                    var updated = new State.Unset<>(copy);
                    if (ref.compareAndSet(s, updated)) {
                        register = Maybe.none();
                        break;
                    }
                } else throw new IllegalArgumentException();
            }
            register.foreach(a -> cb.accept(Either.right(a)));
            return id;
        }

        @Override
        public Higher<F, Unit> complete(final A a) {
            return F.suspend(() -> unsafeComplete(a));
        }

        private Higher<F, Unit> unsafeComplete(A a) {
            var s = ref.get();
            if (s instanceof State.Set) {
                throw new IllegalStateException("Attempting to complete a Deferred that has already been completed");
            } else if (s instanceof State.Unset) {
                if (ref.compareAndSet(s, new State.Set<>(a))) {
                    var waiting = ((State.Unset<A>) s).waiting;
                    if (!waiting.isEmpty()) {
                        return notifyReadersLoop(a, waiting.values());
                    } else {
                        return F.unit();
                    }
                } else {
                    return unsafeComplete(a);
                }
            } else throw new IllegalArgumentException();
        }

        private Higher<F, Unit> notifyReadersLoop(A a, Iterable<Consumer<A>> r) {
            var acc = F.unit();
            for (final Consumer<A> next : r) {
                var task = F.map(F.start(F.delay(() -> {
                    next.accept(a);
                    return Unit.unit();
                })), f -> Unit.unit());
                acc = F.flatMap(acc, u -> task);
            }
            return acc;
        }
    }

    private final static class UncancelableDeferred<F extends Higher, A> extends TryableDeferred<F, A> {
        private final CompletableFuture<A> promise;
        private final Async<F> F;
        private final Higher<F, Unit> asyncBoundary;

        UncancelableDeferred(final CompletableFuture<A> promise, final Async<F> F) {
            this.promise = promise;
            this.F = F;
            this.asyncBoundary = F.async(cb -> cb.accept(Either.right(Unit.unit())));
        }

        @Override
        public Higher<F, A> get() {
            return F.async(cb -> {
                promise.whenComplete((a, t) -> {
                    if (t != null) cb.accept(Either.left(t));
                    else cb.accept(Either.right(a));
                });
            });
        }

        @Override
        public Higher<F, Maybe<A>> tryGet() {
            return F.delay(() -> {
                if (promise.isDone() && !promise.isCompletedExceptionally()) {
                    return Maybe.apply(promise.getNow(null));
                } else {
                    return Maybe.none();
                }
            });
        }

        @Override
        public Higher<F, Unit> complete(final A a) {
            return F.map(asyncBoundary, u -> {
                promise.complete(a);
                return Unit.unit();
            });
        }
    }

    private final static class TransformedDeferred<F extends Higher, G extends Higher, A> extends Deferred<G, A> {
        private final Deferred<F, A> underlying;
        private final FunctionK<F, G> trans;

        TransformedDeferred(final Deferred<F, A> underlying, final FunctionK<F, G> trans) {
            this.underlying = underlying;
            this.trans = trans;
        }

        @Override
        public Higher<G, A> get() {
            return trans.apply(underlying.get());
        }

        @Override
        public Higher<G, Unit> complete(final A a) {
            return trans.apply(underlying.complete(a));
        }
    }
}
