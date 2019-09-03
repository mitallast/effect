package io.mitallast.stream;

import io.mitallast.arrow.FunctionK;
import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.io.ExitCase;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Supplier;
import io.mitallast.maybe.Maybe;


interface FreeC<F extends Higher, R> extends Higher<FreeC<F, ?>, R> {
    @SuppressWarnings("unchecked")
    default <R2> FreeC<F, R2> flatMap(Function1<R, FreeC<F, R2>> f) {
        return new Bind<>(
            this,
            e -> {
                if (e instanceof Result.Pure) {
                    try {
                        return f.apply(((Result.Pure<F, R>) e).r);
                    } catch (Exception err) {
                        return raiseError(err);
                    }
                } else if (e instanceof Result.Interrupted) {
                    return (FreeC<F, R2>) e;
                } else if (e instanceof Result.Fail) {
                    return (FreeC<F, R2>) e;
                } else throw new IllegalArgumentException();
            }
        );
    }

    default <R2> FreeC<F, R2> transformWith(Function1<Result<R>, FreeC<F, R2>> f) {
        return new Bind<>(this, r -> {
            try {
                return f.apply(r);
            } catch (Exception err) {
                return raiseError(err);
            }
        });
    }

    default <R2> FreeC<F, R2> map(Function1<R, R2> f) {
        return new Bind<>(this, r -> Result.map(r, f).asFreeC());
    }

    default FreeC<F, R> handleErrorWith(Function1<Throwable, FreeC<F, R>> h) {
        return new Bind<F, R, R>(this, e -> {
            if (e instanceof Result.Fail) {
                var err = ((Result.Fail) e).error;
                try {
                    return h.apply(err);
                } catch (Exception er) {
                    return raiseError(er);
                }
            } else {
                return e.asFreeC();
            }
        });
    }

    default FreeC<F, R> asHandler(Throwable e) {
        var viewL = ViewL.apply(this);
        if (viewL instanceof Result.Pure) {
            return raiseError(e);
        } else if (viewL instanceof Result.Fail) {
            var err = ((Result.Fail<F, R>) viewL).error;
            return raiseError(CompositeFailure.apply(err, e));
        } else if (viewL instanceof Result.Interrupted) {
            Token ctx = ((Result.Interrupted<F, Token, R>) viewL).context;
            Maybe<Throwable> err = ((Result.Interrupted<F, Token, R>) viewL).deferredError;
            Maybe<Throwable> comp = err.map(er -> CompositeFailure.apply(er, e)).orElse(Maybe.some(e));
            return interrupted(ctx, comp);
        } else if (viewL instanceof ViewL.View) {
            var next = ((ViewL.View<F, Object, R>) viewL).next;
            return next.apply(Result.raiseError(e));
        } else throw new IllegalArgumentException();
    }

    default ViewL<F, R> viewL() {
        return ViewL.apply(this);
    }

    @SuppressWarnings("unchecked")
    default <G extends Higher> FreeC<G, R> translate(FunctionK<F, G> f) {
        return suspend(() -> {
            var view = viewL();
            if (view instanceof ViewL.View) {
                var v = (ViewL.View<F, Object, R>) view;
                return new Bind<G, Object, R>(
                    new Eval<>(v.step).translate(f),
                    e -> v.next.apply(e).translate(f)
                );
            } else if (view instanceof Result) {
                return ((Result<R>) view).asFreeC();
            } else throw new IllegalArgumentException();
        });
    }

    static <F extends Higher> FreeC<F, Unit> unit() {
        return Result.unit.asFreeC();
    }

    static <F extends Higher, A> FreeC<F, A> pure(A a) {
        return Result.pure(a).asFreeC();
    }

    static <F extends Higher, A> FreeC<F, A> eval(Higher<F, A> fa) {
        return new Eval<>(fa);
    }

    static <F extends Higher, A> FreeC<F, A> raiseError(Throwable err) {
        return Result.<A>raiseError(err).asFreeC();
    }

    static <F extends Higher, X, A> FreeC<F, A> interrupted(X interruptContext, Maybe<Throwable> failure) {
        return new Result.Interrupted<>(interruptContext, failure);
    }

    interface Result<R> {
        @SuppressWarnings("unchecked")
        default <F extends Higher> FreeC<F, R> asFreeC() {
            return (FreeC<F, R>) this;
        }

        default ExitCase<Throwable> asExitCase() {
            if (this instanceof Pure) return ExitCase.complete();
            else if (this instanceof Fail) return ExitCase.error(((Fail) this).error);
            else if (this instanceof Interrupted) return ExitCase.canceled();
            else throw new IllegalStateException();
        }

        @SuppressWarnings("unchecked")
        default Result<R> recoverWith(Function1<Throwable, Result<R>> f) {
            if (this instanceof Fail) {
                var err = ((Fail) this).error;
                try {
                    return f.apply(err);
                } catch (Exception e) {
                    return (Result<R>) raiseError(CompositeFailure.apply(err, e));
                }
            } else {
                return this;
            }
        }

        Result<Unit> unit = pure(Unit.unit());

        static <A> Result<A> pure(A a) {
            return new Pure<>(a);
        }

        static <A> Result<A> raiseError(Throwable err) {
            return new Fail<>(err);
        }

        static <A> Result<A> interrupted(Token scopeId, Maybe<Throwable> failure) {
            return new Interrupted<>(scopeId, failure);
        }

        static <R> Result<R> fromEither(Either<Throwable, R> either) {
            return either.fold(
                err -> raiseError(err),
                r -> pure(r)
            );
        }

        @SuppressWarnings("unchecked")
        static <F extends Higher, R> Maybe<Result<R>> unapply(FreeC<F, R> free) {
            if (free instanceof Pure) return Maybe.some((Result<R>) free);
            else if (free instanceof Fail) return Maybe.some((Result<R>) free);
            else if (free instanceof Interrupted) return Maybe.some((Result<R>) free);
            else return Maybe.none();
        }

        final class Pure<F extends Higher, R> implements FreeC<F, R>, Result<R>, ViewL<F, R> {
            final R r;

            public Pure(final R r) {
                this.r = r;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <G extends Higher> FreeC<G, R> translate(final FunctionK<F, G> f) {
                return (FreeC<G, R>) this;
            }
        }

        final class Fail<F extends Higher, R> implements FreeC<F, R>, Result<R>, ViewL<F, R> {
            final Throwable error;

            public Fail(final Throwable error) {
                this.error = error;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <G extends Higher> FreeC<G, R> translate(final FunctionK<F, G> f) {
                return (FreeC<G, R>) this;
            }
        }

        final class Interrupted<F extends Higher, X, R> implements FreeC<F, R>, Result<R>, ViewL<F, R> {
            final X context;
            final Maybe<Throwable> deferredError;

            public Interrupted(final X context, final Maybe<Throwable> deferredError) {
                this.context = context;
                this.deferredError = deferredError;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <G extends Higher> FreeC<G, R> translate(final FunctionK<F, G> f) {
                return (FreeC<G, R>) this;
            }
        }

        @SuppressWarnings("unchecked")
        static <F extends Higher, A, B> Result<B> map(Result<A> fa, Function1<A, B> f) {
            if (fa instanceof Pure) {
                try {
                    return new Pure(f.apply((A) ((Pure) fa).r));
                } catch (Exception e) {
                    return new Fail(e);
                }
            } else if (fa instanceof Fail) {
                return (Fail) fa;
            } else if (fa instanceof Interrupted) {
                return (Result<B>) fa;
            } else throw new IllegalArgumentException();
        }
    }

    final class Eval<F extends Higher, R> implements FreeC<F, R> {
        final Higher<F, R> fr;

        public Eval(final Higher<F, R> fr) {
            this.fr = fr;
        }

        @Override
        public <G extends Higher> FreeC<G, R> translate(final FunctionK<F, G> f) {
            return suspend(() -> {
                try {
                    return new Eval<>(f.apply(fr));
                } catch (Exception e) {
                    return raiseError(e);
                }
            });
        }
    }

    final class Bind<F extends Higher, X, R> implements FreeC<F, R> {
        final FreeC<F, X> fx;
        final Function1<Result<X>, FreeC<F, R>> f;

        public Bind(final FreeC<F, X> fx, final Function1<Result<X>, FreeC<F, R>> f) {
            this.fx = fx;
            this.f = f;
        }
    }

    static <F extends Higher, R> FreeC<F, R> suspend(Supplier<FreeC<F, R>> fr) {
        return FreeC.<F>unit().flatMap(u -> fr.get());
    }

    interface ViewL<F extends Higher, R> {
        final class View<F extends Higher, X, R> implements ViewL<F, R> {
            final Higher<F, X> step;
            final Function1<Result<X>, FreeC<F, R>> next;

            public View(final Higher<F, X> step, final Function1<Result<X>, FreeC<F, R>> next) {
                this.step = step;
                this.next = next;
            }
        }

        @SuppressWarnings("unchecked")
        static <F extends Higher, R> ViewL<F, R> apply(FreeC<F, R> free) {
            if (free instanceof Eval) {
                return new View<>(((Eval<F, R>) free).fr, Result::asFreeC);
            } else if (free instanceof Bind) {
                var b = (Bind<F, Object, R>) free;
                var rOpt = Result.unapply(b.fx);
                if (rOpt.isDefined()) {
                    final Result<Object> r = rOpt.get();
                    return apply(b.f.apply(r));
                } else if (b.fx instanceof Eval) {
                    final Higher<F, Object> fr = ((Eval<F, Object>) b.fx).fr;
                    return new ViewL.View<>(fr, b.f);
                } else if (b.fx instanceof Bind) {
                    var w = ((Bind<F, Object, Object>) b.fx).fx;
                    var g = ((Bind<F, Object, Object>) b.fx).f;
                    return apply(new Bind<>(w, e -> new Bind<>(g.apply(e), b.f)));
                } else throw new IllegalArgumentException();
            } else if (free instanceof Result) {
                return (ViewL<F, R>) free;
            } else throw new IllegalArgumentException();
        }
    }
}