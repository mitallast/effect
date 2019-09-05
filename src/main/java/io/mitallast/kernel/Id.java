package io.mitallast.kernel;

import io.mitallast.categories.Bimonad;
import io.mitallast.categories.CommutativeMonad;
import io.mitallast.categories.Comonad;
import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;
import io.mitallast.maybe.Maybe;

public final class Id<A> implements Higher<Id, A> {
    private final A a;

    private Id(A a) {
        this.a = a;
    }

    public A value() {
        return a;
    }

    public static <A> Id<A> apply(A a) {
        return new Id<>(a);
    }

    private final static Id<Unit> unit = apply(Unit.unit());
    private final static Id<Maybe<Object>> none = apply(Maybe.none());

    public static Id<Unit> unit() {
        return unit;
    }

    @SuppressWarnings("unchecked")
    public static <A> Id<Maybe<A>> none() {
        return (Id<Maybe<A>>) (Id) none;
    }


    public static IdInstances instances() {
        return IdInstances.insance;
    }
}

final class IdInstances implements Bimonad<Id>, CommutativeMonad<Id>, Comonad<Id> {
    public static final IdInstances insance = new IdInstances();

    private IdInstances() {
    }

    @Override
    public <A> Id<A> pure(A a) {
        return Id.apply(a);
    }

    @Override
    public Higher<Id, Unit> unit() {
        return Id.unit();
    }

    @Override
    public <A> Higher<Id, Maybe<A>> none() {
        return Id.none();
    }

    @Override
    public <A> A extract(Higher<Id, A> x) {
        return extract((Id<A>) x);
    }

    public <A> A extract(Id<A> x) {
        return x.value();
    }

    @Override
    public <A, B> Higher<Id, B> flatMap(Higher<Id, A> fa, Function1<A, Higher<Id, B>> f) {
        return f.apply(((Id<A>) fa).value());
    }

    @Override
    public <A, B> Higher<Id, B> coflatMap(Higher<Id, A> fa, Function1<Higher<Id, A>, B> f) {
        return pure(f.apply(fa));
    }

    @Override
    public <A, B> Higher<Id, B> tailRecM(A a, Function1<A, Higher<Id, Either<A, B>>> f) {
        A current = a;
        while (true) {
            var m = (Id<Either<A, B>>) f.apply(current);
            if (m.value().isLeft()) {
                current = m.value().left().get();
            } else {
                return pure(m.value().right().get());
            }
        }
    }
}
