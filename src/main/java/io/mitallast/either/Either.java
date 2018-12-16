package io.mitallast.either;

import io.mitallast.higher.Higher;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface Either<L, R> extends Higher<Either<L, ?>, R> {

    static <A, B> Left<A, B> left(A value) {
        return new Left<>(value);
    }

    static <A, B> Right<A, B> right(B value) {
        return new Right<>(value);
    }

    boolean isLeft();

    boolean isRight();

    LeftProjection<L, R> left();

    RightProjection<L, R> right();

    <C> C fold(Function<L, C> fa, Function<R, C> fb);

    Either<R, L> swap();

    void foreach(Consumer<R> f);

    R getOrElse(Supplier<R> or);

    boolean contains(R elem);

    boolean forall(Predicate<R> f);

    boolean exists(Predicate<R> f);

    <C> Either<L, C> flatMap(Function<R, Either<L, C>> f);

    <C> Either<L, C> map(Function<R, C> f);

    Either<L, R> filterOrElse(Predicate<R> p, Supplier<L> zero);
}
