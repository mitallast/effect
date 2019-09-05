package io.mitallast.stream;

import io.mitallast.arrow.FunctionK;
import io.mitallast.higher.Higher;
import io.mitallast.io.Concurrent;
import io.mitallast.io.ExitCase;
import io.mitallast.io.Sync;
import io.mitallast.io.internals.IOPlatform;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Function2;
import io.mitallast.lambda.Supplier;
import io.mitallast.maybe.Maybe;
import io.mitallast.product.Tuple2;
import io.mitallast.product.Tuple3;

interface Algebra<F extends Higher, O, R> extends Higher<Algebra<F, O, ?>, R> {

    final class Output<F extends Higher, O> implements Algebra<F, O, Unit> {
        private final Chunk<O> values;

        Output(Chunk<O> values) {
            this.values = values;
        }
    }

    final class Step<F extends Higher, O, X> implements
        Algebra<F, O, Maybe<Tuple3<Chunk<X>, Token, FreeC<Algebra<F, X, ?>, Unit>>>> {

        private final FreeC<Algebra<F, X, ?>, Unit> stream;
        private final Maybe<Token> scope;

        Step(FreeC<Algebra<F, X, ?>, Unit> stream, Maybe<Token> scope) {
            this.stream = stream;
            this.scope = scope;
        }
    }

    interface AlgEffect<F extends Higher, O, R> extends Algebra<F, O, R> {
    }


    final class Eval<F extends Higher, O, R> implements AlgEffect<F, O, R> {
        private final Higher<F, R> value;

        Eval(Higher<F, R> value) {
            this.value = value;
        }
    }

    final class Acquire<F extends Higher, O, R> implements AlgEffect<F, O, Tuple2<R, Resource<F>>> {
        private final Higher<F, R> resource;
        private final Function2<R, ExitCase<Throwable>, Higher<F, Unit>> release;

        Acquire(Higher<F, R> resource, Function2<R, ExitCase<Throwable>, Higher<F, Unit>> release) {
            this.resource = resource;
            this.release = release;
        }
    }

    final class OpenScope<F extends Higher, O> implements AlgEffect<F, O, Token> {
        private final Maybe<Concurrent<F>> interruptible;

        OpenScope(Maybe<Concurrent<F>> interruptible) {
            this.interruptible = interruptible;
        }
    }

    final class CloseScope<F extends Higher, O> implements AlgEffect<F, O, Unit> {
        private final Token scopeId;
        private final Maybe<Tuple2<Token, Maybe<Throwable>>> interruptedScope;
        private final ExitCase<Throwable> exitCase;

        CloseScope(Token scopeId,
                   Maybe<Tuple2<Token, Maybe<Throwable>>> interruptedScope,
                   ExitCase<Throwable> exitCase) {
            this.scopeId = scopeId;
            this.interruptedScope = interruptedScope;
            this.exitCase = exitCase;
        }
    }

    final class GetScope<F extends Higher, O> implements AlgEffect<F, O, CompileScope<F>> {
    }

    static <F extends Higher, O> FreeC<Algebra<F, O, ?>, Unit> output(Chunk<O> values) {
        return FreeC.eval(new Output<>(values));
    }

    static <F extends Higher, O> FreeC<Algebra<F, O, ?>, Unit> output1(O value) {
        return output(Chunk.singleton(value));
    }

    static <F extends Higher, O, R> FreeC<Algebra<F, O, ?>, R> eval(Higher<F, R> value) {
        return FreeC.eval(new Eval<>(value));
    }

    static <F extends Higher, O, R> FreeC<Algebra<F, O, ?>, Tuple2<R, Resource<F>>> acquire(
        Higher<F, R> resource,
        Function2<R, ExitCase<Throwable>, Higher<F, Unit>> release
    ) {
        return FreeC.eval(new Acquire<>(resource, release));
    }

    static <F extends Higher, A, B>
    FunctionK<Algebra<F, A, ?>, Algebra<F, B, ?>> mapOutput(Function1<A, B> fun) {
        return new FunctionK<>() {
            @SuppressWarnings("unchecked")
            @Override
            public <R> Higher<Algebra<F, B, ?>, R> apply(final Higher<Algebra<F, A, ?>, R> fa) {
                var alg = (Algebra<F, A, R>) fa;
                if (alg instanceof Output) {
                    var o = (Output<F, A>) alg;
                    return (Algebra<F, B, R>) new Output<F, B>(o.values.map(fun));
                } else {
                    return (Algebra<F, B, R>) alg;
                }
            }
        };
    }

    private static <F extends Higher, O, X>
    FreeC<Algebra<F, X, ?>, Maybe<Tuple3<Chunk<O>, Token, FreeC<Algebra<F, O, ?>, Unit>>>> step(
        FreeC<Algebra<F, O, ?>, Unit> stream,
        Maybe<Token> scopeId
    ) {
        return FreeC.eval(new Step<>(stream, scopeId));
    }

    static <F extends Higher, O>
    FreeC<Algebra<F, O, ?>, Maybe<Stream.StepLeg<F, O>>> stepLeg(Stream.StepLeg<F, O> leg) {
        return Algebra.<F, O, O>step(leg.next, Maybe.some(leg.scopeId))
            .map(o -> o.map(tuple -> {
                var h = tuple.t1();
                var id = tuple.t2();
                var t = tuple.t3();
                return new Stream.StepLeg<>(h, id, t);
            }));
    }

    static <F extends Higher, O>
    FreeC<Algebra<F, O, ?>, Unit> scope(FreeC<Algebra<F, O, ?>, Unit> s) {
        return scope0(s, Maybe.none());
    }

    static <F extends Higher, O>
    FreeC<Algebra<F, O, ?>, Unit> interruptScope(
        FreeC<Algebra<F, O, ?>, Unit> s, Concurrent<F> F
    ) {
        return scope0(s, Maybe.some(F));
    }

    static <F extends Higher, O>
    FreeC<Algebra<F, O, ?>, Token> openScope(Maybe<Concurrent<F>> interruptible) {
        return FreeC.eval(new OpenScope<>(interruptible));
    }

    static <F extends Higher, O>
    FreeC<Algebra<F, O, ?>, Unit> closeScope(
        Token token,
        Maybe<Tuple2<Token, Maybe<Throwable>>> interruptedScope,
        ExitCase<Throwable> exitCase
    ) {
        return FreeC.eval(new CloseScope<>(token, interruptedScope, exitCase));
    }

    private static <F extends Higher, O>
    FreeC<Algebra<F, O, ?>, Unit> scope0(
        FreeC<Algebra<F, O, ?>, Unit> s,
        Maybe<Concurrent<F>> interruptible
    ) {
        return Algebra.<F, O>openScope(interruptible).flatMap(scopeId -> s.transformWith(result -> {
            if (result instanceof FreeC.Result.Pure) {
                return Algebra.closeScope(scopeId, Maybe.none(), ExitCase.complete());
            } else if (result instanceof FreeC.Result.Interrupted) {
                var intr = (FreeC.Result.Interrupted<F, Object, Unit>) result;
                var ctx = intr.context;
                var err = intr.deferredError;
                if (ctx instanceof Token) {
                    var interruptedScopeId = (Token) ctx;
                    return closeScope(scopeId,
                        Maybe.some(new Tuple2<>(interruptedScopeId, err)),
                        ExitCase.canceled()
                    );
                } else {
                    throw new IllegalArgumentException("Impossible context: " + intr.context);
                }
            } else if (result instanceof FreeC.Result.Fail) {
                var fail = (FreeC.Result.Fail<F, Unit>) result;
                return Algebra.<F, O>closeScope(scopeId, Maybe.none(), ExitCase.error(fail.error)).transformWith(rs -> {
                    if (rs instanceof FreeC.Result.Pure) {
                        return FreeC.raiseError(fail.error);
                    } else if (rs instanceof FreeC.Result.Fail) {
                        var fail2 = (FreeC.Result.Fail<F, Unit>) rs;
                        var composed = IOPlatform.composeErrors(fail.error, fail2.error);
                        return FreeC.raiseError(composed);
                    } else if (rs instanceof FreeC.Result.Interrupted) {
                        throw new IllegalStateException();
                    } else throw new IllegalArgumentException();
                });
            } else {
                throw new IllegalArgumentException();
            }
        }));
    }

    static <F extends Higher, O>
    FreeC<Algebra<F, O, ?>, CompileScope<F>> getScope() {
        return FreeC.eval(new GetScope<>());
    }

    static <F extends Higher, O, R>
    FreeC<Algebra<F, O, ?>, R> pure(R r) {
        return FreeC.pure(r);
    }

    static <F extends Higher, O, R>
    FreeC<Algebra<F, O, ?>, R> raiseError(Throwable t) {
        return FreeC.raiseError(t);
    }

    static <F extends Higher, O, R>
    FreeC<Algebra<F, O, ?>, R> suspend(Supplier<FreeC<Algebra<F, O, ?>, R>> f) {
        return FreeC.suspend(f);
    }

    static <F extends Higher, G extends Higher, O>
    FreeC<Algebra<G, O, ?>, Unit> translate(
        FreeC<Algebra<F, O, ?>, Unit> s,
        FunctionK<F, G> u,
        TranslateInterrupt<G> G
    ) {
        return translate0(u, s, G.concurrentInstance());
    }

    static <F extends Higher, X, O>
    FreeC<Algebra<F, X, ?>, Maybe<Tuple2<Chunk<O>, FreeC<Algebra<F, O, ?>, Unit>>>>
    uncons(FreeC<Algebra<F, O, ?>, Unit> s) {
        return Algebra.<F, O, X>step(s, Maybe.none()).map(opt -> opt.map(tuple -> {
            var h = tuple.t1();
            var t = tuple.t3();
            return new Tuple2<>(h, t);
        }));
    }

    static <F extends Higher, O, B>
    Higher<F, B> compile(
        FreeC<Algebra<F, O, ?>, Unit> stream,
        CompileScope<F> scope,
        B init,
        Function2<B, Chunk<O>, B> g,
        Sync<F> F
    ) {
        return F.flatMap(
            Algebra.compileLoop(scope, stream, F),
            opt -> opt.fold(
                () -> F.pure(init),
                t -> {
                    var output = t.t1();
                    var sc = t.t2();
                    var tail = t.t3();
                    try {
                        var b = g.apply(init, output);
                        return compile(tail, sc, b, g, F);
                    } catch (Exception err) {
                        return compile(tail.asHandler(err), sc, init, g, F);
                    }
                }
            )
        );
    }


    static <F extends Higher, O>
    Higher<F, Maybe<Tuple3<Chunk<O>, CompileScope<F>, FreeC<Algebra<F, O, ?>, Unit>>>>
    compileLoop(CompileScope<F> scope, FreeC<Algebra<F, O, ?>, Unit> stream, Sync<F> F) {
        return F.flatMap(
            compileLoopGo(scope, stream, F),
            rr -> compileLoopResult(rr, F)
        );
    }

    private static <F extends Higher, O, X>
    Higher<F, Maybe<Tuple3<Chunk<O>, CompileScope<F>, FreeC<Algebra<F, O, ?>, Unit>>>>
    compileLoopResult(RR<F, X> rr, Sync<F> F) {
        if (rr instanceof RR.Done) {
            return F.none();
        } else if (rr instanceof RR.Out) {
            var out = (RR.Out<F, O>) rr;
            final Chunk<O> head = out.head;
            final CompileScope<F> sc = out.scope;
            final FreeC<Algebra<F, O, ?>, Unit> tail = out.tail;
            return F.pure(Maybe.some(new Tuple3<>(head, sc, tail)));
        } else if (rr instanceof RR.Interrupted) {
            var err = ((RR.Interrupted<F, O>) rr).err;
            return err.fold(
                () -> F.none(),
                e -> F.raiseError(e)
            );
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static <F extends Higher, O, X>
    Higher<F, RR<F, X>>
    compileLoopGo(
        CompileScope<F> scope,
        FreeC<Algebra<F, O, ?>, Unit> stream,
        Sync<F> F
    ) {
        final FreeC.ViewL<Algebra<F, O, ?>, Unit> viewL = stream.viewL();
        if (viewL instanceof FreeC.Result.Pure) {
            return F.pure(new RR.Done<>(scope));
        } else if (viewL instanceof FreeC.Result.Fail) {
            var error = ((FreeC.Result.Fail<Algebra<F, O, ?>, Unit>) viewL).error;
            return F.raiseError(error);
        } else if (viewL instanceof FreeC.Result.Interrupted) {
            var interrupted = (FreeC.Result.Interrupted<Algebra<F, O, ?>, ?, Unit>) viewL;
            if (interrupted.context instanceof Token) {
                var scopeId = (Token) interrupted.context;
                return F.pure(new RR.Interrupted<>(scopeId, interrupted.deferredError));
            } else throw new IllegalStateException("Unexpected interruption context");
        } else if (viewL instanceof FreeC.ViewL.View) {
            var view = (FreeC.ViewL.View<Algebra<F, O, ?>, ?, Unit>) viewL;
            var viewNext = view.next.<FreeC.Result<?>, FreeC<Algebra<F, X, ?>, Unit>>castUnsafe();
            Function2<CompileScope<F>, Supplier<Higher<F, RR<F, X>>>, Higher<F, RR<F, X>>> interruptGuard =
                (sc, next) -> F.flatMap(
                    sc.isInterrupted(),
                    opt -> opt.fold(
                        next,
                        either -> either.fold(
                            err -> compileLoopGo(sc, viewNext.apply(FreeC.Result.raiseError(err)), F),
                            scopeId -> compileLoopGo(sc, viewNext.apply(FreeC.Result.interrupted(scopeId, Maybe.none())), F)
                        )
                    )
                );

            @SuppressWarnings("unchecked")
            var step = (Algebra<F, O, ?>) view.step;
            if (step instanceof Output) {
                @SuppressWarnings("unchecked")
                var output = (Output<F, X>) step;
                return interruptGuard.apply(
                    scope,
                    () -> F.pure(new RR.Out<F, X>(output.values, scope, viewNext.apply(FreeC.Result.unit)))
                );
            } else if (step instanceof Step) {
                @SuppressWarnings("unchecked")
                var u = (Step<F, ?, X>) step;
                // // if scope was specified in step, try to find it, otherwise use the current scope.
                return F.flatMap(
                    u.scope.fold(
                        () -> F.pure(Maybe.some(scope)),
                        scope::findStepScope
                    ),
                    scopeOpt -> scopeOpt.fold(
                        () -> F.raiseError(new IllegalStateException("Fail to find scope for next step")),
                        stepScope -> F.flatMap(
                            F.attempt(compileLoopGo(stepScope, u.stream, F)),
                            either -> either.fold(
                                err -> compileLoopGo(scope, viewNext.apply(FreeC.Result.raiseError(err)), F),
                                rr -> {
                                    if (rr instanceof RR.Done) {
                                        return interruptGuard.apply(
                                            scope,
                                            () -> compileLoopGo(scope, viewNext.apply(FreeC.Result.pure(Maybe.none())), F)
                                        );
                                    } else if (rr instanceof RR.Out) {
                                        var out = (RR.Out<F, X>) rr;
                                        var head = out.head;
                                        var outScope = out.scope;
                                        var tail = out.tail;

                                        // if we originally swapped scopes we want to return the original
                                        // scope back to the go as that is the scope that is expected to be here.

                                        var nextScope = u.scope.fold(() -> outScope, t -> scope);
                                        return interruptGuard.apply(
                                            nextScope,
                                            () -> compileLoopGo(
                                                nextScope,
                                                viewNext.apply(FreeC.Result.pure(Maybe.some(new Tuple3<>(head, outScope.id, tail)))),
                                                F
                                            )
                                        );
                                    } else if (rr instanceof RR.Interrupted) {
                                        var interrupted = (RR.Interrupted<F, X>) rr;
                                        var scopeId = interrupted.scopeId;
                                        var err = interrupted.err;
                                        return compileLoopGo(
                                            scope,
                                            viewNext.apply(FreeC.Result.interrupted(scopeId, err)),
                                            F
                                        );
                                    } else throw new IllegalArgumentException();
                                }
                            )
                        )
                    )
                );
            } else if (step instanceof Eval) {
                @SuppressWarnings("unchecked")
                var eval = (Eval<F, ?, ?>) step;
                return F.flatMap(
                    scope.interruptibleEval(eval.value),
                    ei -> ei.fold(
                        either -> either.fold(
                            err -> compileLoopGo(scope, viewNext.apply(FreeC.Result.raiseError(err)), F),
                            token -> compileLoopGo(scope, viewNext.apply(FreeC.Result.interrupted(token, Maybe.none())), F)
                        ),
                        r -> compileLoopGo(scope, viewNext.apply(FreeC.Result.pure(r)), F)
                    )
                );
            } else if (step instanceof Acquire) {
                @SuppressWarnings("unchecked")
                var acquire = (Acquire<F, ?, Object>) step;
                return interruptGuard.apply(
                    scope,
                    () -> F.flatMap(
                        scope.acquireResource(acquire.resource, acquire.release),
                        r -> {
                            var result = FreeC.Result.fromEither(r.map(t -> (Object) t));
                            return compileLoopGo(scope, viewNext.apply(result), F);
                        }
                    )
                );
            } else if (step instanceof GetScope) {
                return F.suspend(() ->
                    compileLoopGo(scope, viewNext.apply(FreeC.Result.pure(scope)), F)
                );
            } else if (step instanceof OpenScope) {
                @SuppressWarnings("unchecked")
                var open = (OpenScope<F, O>) step;
                return interruptGuard.apply(
                    scope,
                    () -> F.flatMap(
                        scope.open(open.interruptible),
                        e -> e.fold(
                            err -> compileLoopGo(scope, viewNext.apply(FreeC.Result.raiseError(err)), F),
                            childScope -> compileLoopGo(childScope, viewNext.apply(FreeC.Result.pure(childScope.id)), F)
                        )
                    )
                );
            } else if (step instanceof CloseScope) {
                @SuppressWarnings("unchecked")
                var close = (CloseScope<F, ?>) step;
                Function2<CompileScope<F>, ExitCase<Throwable>, Higher<F, RR<F, X>>> closeAndGo =
                    (toClose, ec) -> F.flatMap(
                        toClose.close(ec),
                        r -> F.flatMap(
                            toClose.openAncestor(),
                            ancestor ->
                                close.interruptedScope.fold(
                                    () -> compileLoopGo(ancestor, viewNext.apply(FreeC.Result.fromEither(r)), F),
                                    (Tuple2<Token, Maybe<Throwable>> tuple) -> {
                                        var interruptedScopeId = tuple.t1();
                                        var err = tuple.t2();
                                        var opt = CompositeFailure.fromList(r.swap().toOption().toList().prepend(err.toList()));
                                        if (ancestor.findSelfOrAncestor(interruptedScopeId).isDefined()) {
                                            return compileLoopGo(ancestor, viewNext.apply(FreeC.Result.interrupted(interruptedScopeId, opt)), F);
                                        } else {
                                            return opt.fold(
                                                () -> compileLoopGo(scope, viewNext.apply(FreeC.Result.unit), F),
                                                comp -> compileLoopGo(scope, viewNext.apply(FreeC.Result.raiseError(comp)), F)
                                            );
                                        }
                                    }
                                )
                        )
                    );

                return scope.findSelfOrAncestor(close.scopeId).fold(
                    () -> F.flatMap(
                        scope.findSelfOrChild(close.scopeId),
                        opt -> opt.fold(
                            () -> {
                                var result = close.interruptedScope
                                    .<FreeC.Result<Unit>>map(t -> FreeC.Result.interrupted(t.t1(), t.t2()))
                                    .getOrElse(FreeC.Result.unit);
                                return compileLoopGo(scope, viewNext.apply(result), F);
                            },
                            toClose -> closeAndGo.apply(toClose, close.exitCase)
                        )
                    ),
                    toClose -> closeAndGo.apply(toClose, close.exitCase)
                );

            } else throw new IllegalArgumentException();
        } else throw new IllegalArgumentException();
    }

    static <F extends Higher, O>
    FreeC<Algebra<F, O, ?>, Unit>
    interruptBoundary(
        FreeC<Algebra<F, O, ?>, Unit> stream,
        Token interruptedScope,
        Maybe<Throwable> interruptedError
    ) {
        var viewL = stream.viewL();
        if (viewL instanceof FreeC.Result.Pure) {
            return FreeC.interrupted(interruptedScope, interruptedError);
        } else if (viewL instanceof FreeC.Result.Fail) {
            var failed = (FreeC.Result.Fail<Algebra<F, O, ?>, Unit>) viewL;
            return Algebra.raiseError(
                CompositeFailure.fromList(interruptedError.toList().prepend(failed.error))
                    .getOrElse(failed.error)
            );
        } else if (viewL instanceof FreeC.Result.Interrupted) {
            var interrupted = (FreeC.Result.Interrupted<Algebra<F, O, ?>, ?, Unit>) viewL;
            // impossible
            return FreeC.interrupted(interrupted.context, interrupted.deferredError);
        } else if (viewL instanceof FreeC.ViewL.View) {
            var view = (FreeC.ViewL.View<Algebra<F, O, ?>, ?, Unit>) viewL;
            if (view.step instanceof CloseScope) {
                @SuppressWarnings("unchecked")
                var close = (CloseScope<F, O>) view.step;
                return Algebra.<F, O>closeScope(
                    close.scopeId,
                    Maybe.some(new Tuple2<>(interruptedScope, interruptedError)),
                    close.exitCase
                ).transformWith(view.next.castUnsafe());
            } else {
                var viewNext = view.next.<FreeC.Result<?>, FreeC<Algebra<F, O, ?>, Unit>>castUnsafe();
                return viewNext.apply(FreeC.Result.interrupted(interruptedScope, interruptedError));
            }
        } else throw new IllegalArgumentException();
    }

    private static <F extends Higher, G extends Higher, O>
    FreeC<Algebra<G, O, ?>, Unit> translate0(FunctionK<F, G> fK,
                                             FreeC<Algebra<F, O, ?>, Unit> stream,
                                             Maybe<Concurrent<G>> concurrent
    ) {
        return Algebra.translateStep(stream, true, fK, concurrent);
    }

    private static <F extends Higher, G extends Higher, X>
    FreeC<Algebra<G, X, ?>, Unit> translateStep(FreeC<Algebra<F, X, ?>, Unit> next,
                                                boolean isMainLevel,
                                                FunctionK<F, G> fK,
                                                Maybe<Concurrent<G>> concurrent) {
        var viewL = next.viewL();
        if (viewL instanceof FreeC.Result.Pure) {
            return FreeC.unit();
        } else if (viewL instanceof FreeC.Result.Fail) {
            var failed = (FreeC.Result.Fail<Algebra<F, X, ?>, Unit>) viewL;
            return raiseError(failed.error);
        } else if (viewL instanceof FreeC.Result.Interrupted) {
            var intr = (FreeC.Result.Interrupted<Algebra<F, X, ?>, ?, ?>) viewL;
            return FreeC.interrupted(intr.context, intr.deferredError);
        } else if (viewL instanceof FreeC.ViewL.View) {
            var view = (FreeC.ViewL.View<Algebra<F, X, ?>, ?, Unit>) viewL;
            var viewNext = view.next.<FreeC.Result<?>, FreeC<Algebra<F, X, ?>, Unit>>castUnsafe();

            if (view.step instanceof Output) {
                @SuppressWarnings("unchecked")
                var output = (Output<F, X>) view.step;
                return Algebra.<G, X>output(output.values).transformWith(r -> {
                    if (r instanceof FreeC.Result.Pure) {
                        if (isMainLevel) {
                            return Algebra.translateStep(viewNext.apply(r), isMainLevel, fK, concurrent);
                        } else {
                            @SuppressWarnings("unchecked")
                            var casted = (FreeC<Algebra<G, X, ?>, Unit>) (FreeC<?, Unit>) viewNext.apply(r);
                            return casted;
                        }
                    } else if (r instanceof FreeC.Result.Fail) {
                        return Algebra.translateStep(viewNext.apply(r), isMainLevel, fK, concurrent);
                    } else if (r instanceof FreeC.Result.Interrupted) {
                        return Algebra.translateStep(viewNext.apply(r), isMainLevel, fK, concurrent);
                    } else throw new IllegalArgumentException();
                });
            } else if (view.step instanceof Step) {
                @SuppressWarnings("unchecked")
                var step = (Step<F, X, ?>) view.step;
                return Algebra.translateViewStep(view, step, isMainLevel, fK, concurrent);
            } else if (view.step instanceof AlgEffect) {
                @SuppressWarnings("unchecked")
                var alg = (AlgEffect<F, X, ?>) view.step;
                return translateAlgEffectStep(view, alg, isMainLevel, fK, concurrent);
            } else throw new IllegalArgumentException();

        } else throw new IllegalArgumentException();
    }

    private static <F extends Higher, G extends Higher, X, x, y>
    FreeC<Algebra<G, X, ?>, Unit>
    translateViewStep(
        FreeC.ViewL.View<Algebra<F, X, ?>, y, Unit> view,
        Step<F, ?, x> step,
        boolean isMainLevel,
        FunctionK<F, G> fK,
        Maybe<Concurrent<G>> concurrent
    ) {
        var viewNext = view.next.<FreeC.Result<?>, FreeC<Algebra<F, X, ?>, Unit>>castUnsafe();
        return new FreeC.Eval<Algebra<G, X, ?>, Maybe<Tuple3<Chunk<x>, Token, FreeC<Algebra<G, x, ?>, Unit>>>>(
            new Step<>(
                Algebra.translateStep(step.stream, false, fK, concurrent),
                step.scope
            )
        ).transformWith(r -> Algebra.translateStep(viewNext.apply(r), isMainLevel, fK, concurrent));
    }

    private static <F extends Higher, G extends Higher, X, r, y>
    FreeC<Algebra<G, X, ?>, Unit>
    translateAlgEffectStep(FreeC.ViewL.View<Algebra<F, X, ?>, y, Unit> view,
                           AlgEffect<F, X, r> alg,
                           boolean isMainLevel,
                           FunctionK<F, G> fK,
                           Maybe<Concurrent<G>> concurrent
    ) {
        var viewNext = view.next.<FreeC.Result<?>, FreeC<Algebra<F, X, ?>, Unit>>castUnsafe();
        return new FreeC.Eval<>(translateAlgEffect(alg, fK, concurrent))
            .transformWith(r -> translateStep(viewNext.apply(r), isMainLevel, fK, concurrent));
    }

    @SuppressWarnings("unchecked")
    private static <F extends Higher, G extends Higher, O, R>
    AlgEffect<G, O, R> translateAlgEffect(
        AlgEffect<F, O, R> self,
        FunctionK<F, G> fK,
        Maybe<Concurrent<G>> concurrent
    ) {
        if (self instanceof Acquire) {
            var a = (Acquire<F, O, Object>) self;
            return (AlgEffect<G, O, R>) new Acquire<G, O, Object>(
                fK.apply(a.resource), (r, ec) -> fK.apply(a.release.apply(r, ec))
            );
        } else if (self instanceof Eval) {
            var e = (Eval<F, O, R>) self;
            return new Eval<>(fK.apply(e.value));
        } else if (self instanceof OpenScope) {
            return (AlgEffect<G, O, R>) new OpenScope<G, O>(concurrent);
        } else if (self instanceof CloseScope) {
            return (AlgEffect<G, O, R>) self;
        } else if (self instanceof GetScope) {
            return (AlgEffect<G, O, R>) self;
        } else throw new IllegalArgumentException();
    }
}

abstract class RR<F extends Higher, X> {
    private RR() {
    }

    final static class Done<F extends Higher, X> extends RR<F, X> {
        final CompileScope<F> scope;

        Done(final CompileScope<F> scope) {
            this.scope = scope;
        }
    }

    final static class Out<F extends Higher, X> extends RR<F, X> {
        final Chunk<X> head;
        final CompileScope<F> scope;
        final FreeC<Algebra<F, X, ?>, Unit> tail;

        Out(final Chunk<X> head, final CompileScope<F> scope, final FreeC<Algebra<F, X, ?>, Unit> tail) {
            this.head = head;
            this.scope = scope;
            this.tail = tail;
        }
    }

    final static class Interrupted<F extends Higher, X> extends RR<F, X> {
        final Token scopeId;
        final Maybe<Throwable> err;

        Interrupted(final Token scopeId, final Maybe<Throwable> err) {
            this.scopeId = scopeId;
            this.err = err;
        }
    }
}