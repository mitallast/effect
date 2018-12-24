package io.mitallast.either;

import io.mitallast.lambda.Function1;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface RightProjection<A, B> {
    B get();

    void foreach(Consumer<B> f);

    B getOrElse(Supplier<B> or);

    boolean forall(Predicate<B> f);

    boolean exists(Predicate<B> f);

    Either<A, B> flatMap(Function1<B, Either<A, B>> f);

    Either<A, B> map(Function1<B, B> f);

    Optional<Either<A, B>> filter(Predicate<B> f);
}
