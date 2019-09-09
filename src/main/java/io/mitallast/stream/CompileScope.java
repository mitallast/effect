package io.mitallast.stream;

import io.mitallast.concurrent.Deferred;
import io.mitallast.concurrent.Ref;
import io.mitallast.data.Chain;
import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.io.Concurrent;
import io.mitallast.io.ExitCase;
import io.mitallast.io.Sync;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Function2;
import io.mitallast.list.List;
import io.mitallast.maybe.Maybe;
import io.mitallast.product.Product2;
import io.mitallast.product.Tuple;
import io.mitallast.product.Tuple2;

final class CompileScope<F extends Higher> {
    final Token id;
    private final Maybe<CompileScope<F>> parent;
    private final Maybe<InterruptContext<F>> interruptible;
    private final Ref<F, State<F>> state;
    private final Sync<F> F;

    CompileScope(Token id,
                 Maybe<CompileScope<F>> parent,
                 Maybe<InterruptContext<F>> interruptible,
                 Sync<F> F) {
        this.id = id;
        this.parent = parent;
        this.interruptible = interruptible;
        this.state = Ref.unsafe(State.initial(), F);
        this.F = F;
    }

    public Higher<F, Unit> register(Resource<F> resource) {
        return state.update(s -> s.addResource(resource));
    }

    public Higher<F, Either<Throwable, CompileScope<F>>> open(Maybe<Concurrent<F>> interruptible) {
        var newScopeId = new Token();
        var self = this;
        return F.flatMap(
            this.interruptible.fold(
                () -> F.pure(InterruptContext.unsafeFromInterruptible(interruptible, newScopeId, F)),
                parentICtx -> F.map(parentICtx.childContext(interruptible, newScopeId, F), Maybe::some)
            ),
            iCtx ->
                F.flatMap(
                    state.modify(s -> {
                        if (!s.open) return Tuple.of(s, Maybe.<CompileScope<F>>none());
                        else {
                            var scope = new CompileScope<F>(newScopeId, Maybe.some(self), iCtx, F);
                            return Tuple.of(s.addChild(scope), Maybe.some(scope));
                        }
                    }),
                    opt -> opt.fold(
                        () ->
                            self.parent.fold(
                                () -> F.pure(Either.left(new IllegalStateException("cannot re-open root scope"))),
                                parent ->
                                    F.flatMap(
                                        self.interruptible.map(i -> i.cancelParent).getOrElse(F.unit()),
                                        u -> parent.open(interruptible)
                                    )
                            ),
                        s -> F.pure(Either.right(s))
                    )
                )
        );
    }

    public <R> Higher<F, Either<Throwable, Tuple2<R, Resource<F>>>> acquireResource(
        Higher<F, R> fr,
        Function2<R, ExitCase<Throwable>, Higher<F, Unit>> release
    ) {
        var resource = Resource.create(F);
        return F.flatMap(
            F.attempt(fr),
            e -> e.fold(
                err -> F.pure(Either.left(err)),
                r ->
                    F.flatMap(
                        resource.acquired(ec -> F.suspend(() -> release.apply(r, ec))),
                        result -> {
                            if (result.exists(i -> i)) {
                                return F.map(
                                    register(resource),
                                    ignore -> Either.right(Tuple.of(r, resource))
                                );
                            } else {
                                return F.pure(Either.left(result.swap().getOrElse(AcquireAfterScopeClosed::new)));
                            }
                        }
                    )
            )
        );
    }

    public Higher<F, Unit> releaseChildScope(Token id) {
        return state.update(s -> s.unregisterChild(id));
    }

    public Higher<F, Chain<Resource<F>>> resources() {
        return F.map(state.get(), s -> s.resources);
    }

    private <A> Higher<F, Either<Throwable, Unit>> traverseError(
        Chain<A> ca,
        Function1<A, Higher<F, Either<Throwable, Unit>>> f
    ) {
        return F.map(
            Chain.instances().<F, A, Either<Throwable, Unit>>traverse(ca, f.castUnsafe(), F),
            results -> {
                var cr = (Chain<Either<Throwable, Unit>>) results;
                List<Throwable> errors = cr.foldLeft(List.nil(), (z, a) -> a.fold(z::prepend, i -> z));
                return CompositeFailure.fromList(errors).toLeft(Unit::unit);
            }
        );
    }

    public Higher<F, Either<Throwable, Unit>> close(ExitCase<Throwable> ec) {
        var self = this;
        return F.flatMap(
            state.modify(s -> Tuple.of(s.close(), s)),
            previous ->
                F.flatMap(
                    traverseError(previous.children, s -> s.close(ec)),
                    resultChildren ->
                        F.flatMap(
                            traverseError(previous.resources, s -> s.release(ec)),
                            resultResources ->
                                F.map(
                                    self.interruptible.map(c -> c.cancelParent).getOrElse(F.unit()),
                                    unit -> {
                                        List<Throwable> a = resultChildren.fold(List::of, ii -> List.nil());
                                        List<Throwable> b = resultResources.fold(List::of, ii -> List.nil());
                                        List<Throwable> results = a.prepend(b);
                                        return CompositeFailure.fromList(results).toLeft(Unit::unit);
                                    }
                                )
                        )
                )
        );
    }

    public Higher<F, CompileScope<F>> openAncestor() {
        var self = this;
        return this.parent.fold(
            () -> F.pure(self),
            parent -> F.flatMap(
                parent.state.get(),
                s -> {
                    if (s.open) return F.pure(parent);
                    else return parent.openAncestor();
                }
            )
        );
    }

    private Chain<CompileScope<F>> ancestors() {
        var curr = this;
        var acc = Chain.<CompileScope<F>>empty();
        while (true) {
            var parent = curr.parent;
            if (parent.isDefined()) {
                curr = parent.get();
                acc = acc.prepend(parent.get());
            } else {
                return acc;
            }
        }
    }

    public Maybe<CompileScope<F>> findAncestor(Token scopeId) {
        var curr = this;
        while (true) {
            if (curr.id == scopeId) {
                return Maybe.some(curr);
            } else {
                if (curr.parent.isDefined()) {
                    curr = curr.parent.get();
                } else {
                    return Maybe.none();
                }
            }
        }
    }

    public Maybe<CompileScope<F>> findSelfOrAncestor(Token scopeId) {
        return findAncestor(scopeId);
    }

    private Higher<F, Maybe<CompileScope<F>>> findSelfOrChildGo(Token scopeId, Chain<CompileScope<F>> scopes) {
        return scopes.uncons().fold(
            () -> F.none(),
            t -> {
                var scope = t.t1();
                var tail = t.t2();
                if (scope.id == scopeId) {
                    return F.pure(Maybe.some(scope));
                } else {
                    return F.flatMap(
                        scope.state.get(),
                        s -> {
                            if (s.children.isEmpty()) {
                                return findSelfOrChildGo(scopeId, tail);
                            } else {
                                return F.flatMap(
                                    findSelfOrChildGo(scopeId, s.children),
                                    m -> m.fold(
                                        () -> findSelfOrChildGo(scopeId, tail),
                                        sc -> F.pure(Maybe.some(sc))
                                    )
                                );
                            }
                        }
                    );
                }
            }
        );
    }

    public Higher<F, Maybe<CompileScope<F>>> findSelfOrChild(Token scopeId) {
        if (this.id == scopeId) {
            return F.pure(Maybe.some(this));
        } else {
            return F.flatMap(
                state.get(),
                s -> findSelfOrChildGo(scopeId, s.children)
            );
        }
    }

    private CompileScope<F> findStepScope(Token scopeId, CompileScope<F> scope) {
        return scope.parent.fold(
            () -> scope,
            parent -> findStepScope(scopeId, parent)
        );
    }

    public Higher<F, Maybe<CompileScope<F>>> findStepScope(Token scopeId) {
        var self = this;
        if (self.id == scopeId) {
            return F.pure(Maybe.some(self));
        } else {
            return this.parent.fold(
                () -> findSelfOrChild(scopeId),
                parent -> F.flatMap(
                    parent.findSelfOrChild(scopeId),
                    m -> m.fold(
                        () -> {
                            var scope = self;
                            while (true) {
                                var pr = scope.parent;
                                if (pr.isDefined()) {
                                    scope = pr.get();
                                } else break;
                            }
                            return scope.findSelfOrChild(scopeId);
                        },
                        scope -> F.pure(Maybe.some(scope))
                    )
                )
            );
        }
    }

    public Higher<F, Maybe<Scope.Lease<F>>> lease() {
        var self = this;
        return F.flatMap(
            state.get(),
            s -> {
                if (!s.open) return F.none();
                else {
                    var allScopes = (s.children.prepend(self)).append(ancestors());
                    return F.flatMap(
                        Chain.instances().<F, CompileScope<F>, Resource<F>>flatTraverse(
                            allScopes,
                            ss -> ss.resources().castTUnsafe(),
                            F,
                            Chain.instances()
                        ),
                        allResources ->
                            F.map(
                                Chain.traverseFilter().traverseFilter(allResources, Resource::lease, F),
                                allLeases -> {
                                    var lease = new Scope.Lease<F>() {
                                        @Override
                                        public Higher<F, Either<Throwable, Unit>> cancel() {
                                            return self.traverseError((Chain<Scope.Lease<F>>) allLeases, Scope.Lease::cancel);
                                        }
                                    };
                                    return Maybe.some(lease);
                                }
                            )
                    );
                }
            }
        );
    }

    public Higher<F, Unit> interrupt(Either<Throwable, Unit> cause) {
        return interruptible.fold(
            () -> F.raiseError(new IllegalStateException("Scope#interrupt called for Scope that cannot be interrupted")),
            iCtx -> {
                var interruptCause = cause.map(u -> iCtx.interruptRoot);
                return F.guarantee(
                    iCtx.deferred.complete(interruptCause),
                    iCtx.ref.update(opt -> opt.orElse(Maybe.some(interruptCause)))
                );
            }
        );
    }

    public Higher<F, Maybe<Either<Throwable, Token>>> isInterrupted() {
        if (interruptible.isEmpty()) {
            return F.none();
        } else {
            return interruptible.get().ref.get();
        }
    }

    <A> Higher<F, Either<Either<Throwable, Token>, A>> interruptibleEval(Higher<F, A> f) {
        return interruptible.fold(
            () -> F.map(F.attempt(f), e -> e.leftMap(Either::left)),
            iCtx -> F.map(
                iCtx.concurrent.race(iCtx.deferred.get(), F.attempt(f)),
                e -> e.fold(
                    other -> Either.left(other),
                    result -> result.leftMap(Either::left)
                )
            )
        );
    }

    public static <F extends Higher> Higher<F, CompileScope<F>> newRoot(Sync<F> F) {
        return F.delay(() -> new CompileScope<>(new Token(), Maybe.none(), Maybe.none(), F));
    }

    final static class State<F extends Higher> {
        private final boolean open;
        private final Chain<Resource<F>> resources;
        private final Chain<CompileScope<F>> children;

        State(final boolean open, final Chain<Resource<F>> resources, final Chain<CompileScope<F>> children) {
            this.open = open;
            this.resources = resources;
            this.children = children;
        }

        public Tuple2<State<F>, Maybe<Resource<F>>> unregisterResource(Token id) {
            return resources.deleteFirst(r -> r.id() == id).fold(
                () -> Tuple.of(this, Maybe.none()),
                t -> Tuple.of(this.withResources(t.t2()), Maybe.some(t.t1()))
            );
        }

        public State<F> unregisterChild(Token id) {
            return withChildren(
                children.deleteFirst(c -> c.id == id)
                    .map(Product2::t2)
                    .getOrElse(children)
            );
        }

        public State<F> close() {
            return State.closed();
        }

        public State<F> withResources(Chain<Resource<F>> resources) {
            return new State<>(open, resources, children);
        }

        public State<F> withChildren(Chain<CompileScope<F>> children) {
            return new State<>(open, resources, children);
        }

        public State<F> addResource(Resource<F> resource) {
            return withResources(resources.prepend(resource));
        }

        public State<F> addChild(CompileScope<F> child) {
            return withChildren(children.prepend(child));
        }

        private static final State<Higher> initial = new State<>(true, Chain.empty(), Chain.empty());

        private static final State<Higher> closed = new State<>(false, Chain.empty(), Chain.empty());


        @SuppressWarnings("unchecked")
        public static <F extends Higher> State<F> initial() {
            return (State<F>) initial;
        }

        @SuppressWarnings("unchecked")
        public static <F extends Higher> State<F> closed() {
            return (State<F>) closed;
        }
    }

    final static class InterruptContext<F extends Higher> {
        private final Concurrent<F> concurrent;
        private final Deferred<F, Either<Throwable, Token>> deferred;
        private final Ref<F, Maybe<Either<Throwable, Token>>> ref;
        private final Token interruptRoot;
        private final Higher<F, Unit> cancelParent;

        InterruptContext(final Concurrent<F> concurrent,
                         final Deferred<F, Either<Throwable, Token>> deferred,
                         final Ref<F, Maybe<Either<Throwable, Token>>> ref,
                         final Token interruptRoot,
                         final Higher<F, Unit> cancelParent) {
            this.concurrent = concurrent;
            this.deferred = deferred;
            this.ref = ref;
            this.interruptRoot = interruptRoot;
            this.cancelParent = cancelParent;
        }

        public Higher<F, InterruptContext<F>> childContext(
            Maybe<Concurrent<F>> interruptible,
            Token newScopeId,
            Sync<F> F
        ) {
            return interruptible
                .map(concurrent ->
                    F.flatMap(
                        concurrent.start(deferred.get()),
                        fiber -> {
                            var context = new InterruptContext<F>(
                                concurrent,
                                Deferred.unsafe(concurrent),
                                Ref.unsafe(Maybe.none(), F),
                                newScopeId,
                                fiber.cancel()
                            );
                            return F.map(
                                concurrent.start(
                                    F.flatMap(
                                        fiber.join(),
                                        interrupt ->
                                            F.flatMap(
                                                context.ref.update(m -> m.orElse(Maybe.some(interrupt))),
                                                u -> F.map(
                                                    F.attempt(context.deferred.complete(interrupt)),
                                                    uu -> Unit.unit()
                                                )
                                            )
                                    )
                                ),
                                c -> context
                            );
                        }
                    )
                )
                .getOrElse(F.pure(withCancelParent(F.unit())));
        }

        public InterruptContext<F> withCancelParent(Higher<F, Unit> cancelParent) {
            return new InterruptContext<>(concurrent, deferred, ref, interruptRoot, cancelParent);
        }

        public static <F extends Higher> Maybe<InterruptContext<F>> unsafeFromInterruptible(
            Maybe<Concurrent<F>> interruptible, Token newScopeId, Sync<F> F
        ) {
            return interruptible.map(concurrent ->
                new InterruptContext<>(
                    concurrent,
                    Deferred.unsafe(concurrent),
                    Ref.unsafe(Maybe.none(), F),
                    newScopeId,
                    F.unit()
                )
            );
        }
    }
}
