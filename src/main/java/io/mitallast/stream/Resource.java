package io.mitallast.stream;

import io.mitallast.concurrent.Ref;
import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.io.ExitCase;
import io.mitallast.io.Sync;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;
import io.mitallast.maybe.Maybe;
import io.mitallast.product.Tuple;
import io.mitallast.product.Tuple2;

abstract class Resource<F extends Higher> {
    private Resource() {
    }

    public abstract Token id();

    public abstract Higher<F, Either<Throwable, Unit>> release(ExitCase<Throwable> ec);

    public abstract Higher<F, Either<Throwable, Boolean>> acquired(Function1<ExitCase<Throwable>, Higher<F, Unit>> finalizer);

    public abstract Higher<F, Maybe<Scope.Lease<F>>> lease();

    final static class State<F extends Higher> {
        private final boolean open;
        private final Maybe<Function1<ExitCase<Throwable>, Higher<F, Either<Throwable, Unit>>>> finalizer;
        private final int leases;

        State(boolean open, Maybe<Function1<ExitCase<Throwable>, Higher<F, Either<Throwable, Unit>>>> finalizer, int leases) {
            this.open = open;
            this.finalizer = finalizer;
            this.leases = leases;
        }

        public boolean isFinished() {
            return !open && leases == 0;
        }

        public State<F> withOpen(boolean open) {
            return new State<>(open, finalizer, leases);
        }

        public State<F> withoutFinalizer() {
            return new State<>(open, Maybe.none(), leases);
        }

        public State<F> withFinalizer(Function1<ExitCase<Throwable>, Higher<F, Either<Throwable, Unit>>> finalizer) {
            return new State<>(open, Maybe.some(finalizer), leases);
        }

        public State<F> withLeases(int leases) {
            return new State<>(open, finalizer, leases);
        }

        public State<F> incLeases() {
            return new State<>(open, finalizer, leases + 1);
        }
    }

    static <F extends Higher> State<F> initial() {
        return new State<>(true, Maybe.none(), 0);
    }

    static <F extends Higher> Resource<F> create(Sync<F> F) {
        return new Resource<F>() {
            final Ref<F, State<F>> state = Ref.unsafe(initial(), F);
            final Token id = new Token();
            final Higher<F, Either<Throwable, Unit>> pru = F.pure(Either.right(Unit.unit()));

            @Override
            public Token id() {
                return id;
            }

            @Override
            public Higher<F, Either<Throwable, Unit>> release(ExitCase<Throwable> ec) {
                return F.flatMap(
                    state.<Maybe<Function1<ExitCase<Throwable>, Higher<F, Either<Throwable, Unit>>>>>modify(s -> {
                            if (s.leases != 0) {
                                return Tuple.of(s.withOpen(false), Maybe.none());
                            } else {
                                return Tuple.of(s.withOpen(false).withoutFinalizer(), s.finalizer);
                            }
                        }
                    ),
                    finalizer -> finalizer.map(f -> f.apply(ec)).getOrElse(pru)
                );
            }

            @Override
            public Higher<F, Either<Throwable, Boolean>> acquired(Function1<ExitCase<Throwable>, Higher<F, Unit>> finalizer) {
                return F.flatten(state.modify(s -> {
                    if (s.isFinished()) {
                        // state is closed and there are no leases, finalizer has to be invoked right away
                        return Tuple.of(s, F.attempt(F.as(finalizer.apply(ExitCase.complete()), false)));
                    } else {
                        // either state is open, or leases are present, either release or `Lease#cancel` will run the finalizer
                        return Tuple.of(s.withFinalizer(ec -> F.attempt(finalizer.apply(ec))), F.pure(Either.right(true)));
                    }
                }));
            }

            @Override
            public Higher<F, Maybe<Scope.Lease<F>>> lease() {
                return state.modify(s -> {
                    if (s.open) {
                        return Tuple.of(s.incLeases(), Maybe.some(new TheLease()));
                    } else {
                        return Tuple.of(s, Maybe.none());
                    }
                });
            }

            class TheLease extends Scope.Lease<F> {
                @Override
                public Higher<F, Either<Throwable, Unit>> cancel() {
                    return F.flatMap(
                        state.<State<F>>modify(s -> {
                            var now = s.incLeases();
                            return Tuple.of(now, now);
                        }),
                        now -> {
                            if (now.isFinished()) {
                                return F.flatten(state.modify(s -> Tuple.of(
                                    s.withoutFinalizer(),
                                    s.finalizer.map(ff -> ff.apply(ExitCase.complete())).getOrElse(pru))));
                            } else return pru;
                        }
                    );
                }
            }
        };
    }
}
