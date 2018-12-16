package io.mitallast.either;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class Left<A, B> implements Either<A, B> {
    private final A value;

    Left(A value) {
        this.value = value;
    }

    @Override
    public boolean isLeft() {
        return true;
    }

    @Override
    public boolean isRight() {
        return false;
    }

    public A value() {
        return value;
    }

    @Override
    public <C> C fold(Function<A, C> fa, Function<B, C> fb) {
        return fa.apply(value);
    }

    @Override
    public Either<B, A> swap() {
        return new Right<>(value);
    }

    @Override
    public void foreach(Consumer<B> f) {
    }

    @Override
    public B getOrElse(Supplier<B> or) {
        return or.get();
    }

    @Override
    public boolean contains(B elem) {
        return false;
    }

    @Override
    public boolean forall(Predicate<B> f) {
        return true;
    }

    @Override
    public boolean exists(Predicate<B> f) {
        return false;
    }

    @Override
    public <C> Either<A, C> flatMap(Function<B, Either<A, C>> f) {
        return new Left<>(value);
    }

    @Override
    public <C> Either<A, C> map(Function<B, C> f) {
        return new Left<>(value);
    }

    @Override
    public Either<A, B> filterOrElse(Predicate<B> p, Supplier<A> zero) {
        return new Left<>(value);
    }

    @Override
    public LeftProjection<A, B> left() {
        return new LeftProjectionImpl<>(this);
    }

    @Override
    public RightProjection<A, B> right() {
        return new RightProjectionImpl<>(this);
    }

    @Override
    public String toString() {
        return "Left(" + value + ')';
    }

    private static class LeftProjectionImpl<A, B> implements LeftProjection<A, B> {
        private final Left<A, B> e;

        private LeftProjectionImpl(Left<A, B> e) {
            this.e = e;
        }

        @Override
        public A get() {
            return e.value;
        }

        @Override
        public void foreach(Consumer<A> f) {
            f.accept(e.value);
        }

        @Override
        public A getOrElse(Supplier<A> or) {
            return e.value;
        }

        @Override
        public boolean forall(Predicate<A> f) {
            return f.test(e.value);
        }

        @Override
        public boolean exists(Predicate<A> f) {
            return f.test(e.value);
        }

        @Override
        public Either<A, B> flatMap(Function<A, Either<A, B>> f) {
            return null;
        }

        @Override
        public Either<A, B> map(Function<A, A> f) {
            return new Left<>(f.apply(e.value));
        }

        @Override
        public Optional<Either<A, B>> filter(Predicate<A> f) {
            if (f.test(e.value)) {
                return Optional.of(e);
            } else {
                return Optional.empty();
            }
        }
    }

    private static class RightProjectionImpl<A, B> implements RightProjection<A, B> {
        private final Left<A, B> e;

        private RightProjectionImpl(Left<A, B> e) {
            this.e = e;
        }

        @Override
        public B get() {
            throw new NoSuchElementException("Either.right.get on Left");
        }

        @Override
        public void foreach(Consumer<B> f) {
        }

        @Override
        public B getOrElse(Supplier<B> or) {
            return or.get();
        }

        @Override
        public boolean forall(Predicate<B> f) {
            return true;
        }

        @Override
        public boolean exists(Predicate<B> f) {
            return false;
        }

        @Override
        public Either<A, B> flatMap(Function<B, Either<A, B>> f) {
            return new Left<>(e.value);
        }

        @Override
        public Either<A, B> map(Function<B, B> f) {
            return new Left<>(e.value);
        }

        @Override
        public Optional<Either<A, B>> filter(Predicate<B> f) {
            return Optional.empty();
        }
    }
}
