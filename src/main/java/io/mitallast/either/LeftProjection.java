package io.mitallast.either;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

interface LeftProjection<A, B> {
    A get();

    void foreach(Consumer<A> f);

    A getOrElse(Supplier<A> or);

    boolean forall(Predicate<A> f);

    boolean exists(Predicate<A> f);

    Either<A, B> flatMap(Function<A, Either<A, B>> f);

    Either<A, B> map(Function<A, A> f);

    Optional<Either<A, B>> filter(Predicate<A> f);
}
