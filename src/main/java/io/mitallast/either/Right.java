package io.mitallast.either;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class Right<A, B> implements Either<A, B> {
    private final B value;

    Right(B value) {
        this.value = value;
    }

    @Override
    public boolean isLeft() {
        return false;
    }

    @Override
    public boolean isRight() {
        return true;
    }

    public B value() {
        return value;
    }

    @Override
    public <C> C fold(Function<A, C> fa, Function<B, C> fb) {
        return fb.apply(value);
    }

    @Override
    public Either<B, A> swap() {
        return new Left<>(value);
    }

    @Override
    public void foreach(Consumer<B> f) {
        f.accept(value);
    }

    @Override
    public B getOrElse(Supplier<B> or) {
        return value;
    }

    @Override
    public boolean contains(B elem) {
        return Objects.equals(value, elem);
    }

    @Override
    public boolean forall(Predicate<B> f) {
        return f.test(value);
    }

    @Override
    public boolean exists(Predicate<B> f) {
        return f.test(value);
    }

    @Override
    public <C> Either<A, C> flatMap(Function<B, Either<A, C>> f) {
        return f.apply(value);
    }

    @Override
    public <C> Either<A, C> map(Function<B, C> f) {
        return new Right<>(f.apply(value));
    }

    @Override
    public Either<A, B> filterOrElse(Predicate<B> p, Supplier<A> zero) {
        if (p.test(value)) {
            return this;
        } else {
            return new Left<>(zero.get());
        }
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
        return "Right(" + value + ')';
    }

    private static class LeftProjectionImpl<A, B> implements LeftProjection<A, B> {
        private final Right<A, B> e;

        private LeftProjectionImpl(Right<A, B> e) {
            this.e = e;
        }

        @Override
        public A get() {
            throw new NoSuchElementException("Either.left.get on Right");
        }

        @Override
        public void foreach(Consumer<A> f) {
        }

        @Override
        public A getOrElse(Supplier<A> or) {
            return or.get();
        }

        @Override
        public boolean forall(Predicate<A> f) {
            return true;
        }

        @Override
        public boolean exists(Predicate<A> f) {
            return false;
        }

        @Override
        public Either<A, B> flatMap(Function<A, Either<A, B>> f) {
            return new Right<>(e.value);
        }

        @Override
        public Either<A, B> map(Function<A, A> f) {
            return new Right<>(e.value);
        }

        @Override
        public Optional<Either<A, B>> filter(Predicate<A> f) {
            return Optional.empty();
        }
    }

    private static class RightProjectionImpl<A, B> implements RightProjection<A, B> {
        private final Right<A, B> e;

        private RightProjectionImpl(Right<A, B> e) {
            this.e = e;
        }

        @Override
        public B get() {
            return e.value;
        }

        @Override
        public void foreach(Consumer<B> f) {
            f.accept(e.value);
        }

        @Override
        public B getOrElse(Supplier<B> or) {
            return e.value;
        }

        @Override
        public boolean forall(Predicate<B> f) {
            return f.test(e.value);
        }

        @Override
        public boolean exists(Predicate<B> f) {
            return f.test(e.value);
        }

        @Override
        public Either<A, B> flatMap(Function<B, Either<A, B>> f) {
            return f.apply(e.value);
        }

        @Override
        public Either<A, B> map(Function<B, B> f) {
            return new Right<>(f.apply(e.value));
        }

        @Override
        public Optional<Either<A, B>> filter(Predicate<B> f) {
            if (f.test(e.value)) {
                return Optional.of(e);
            } else {
                return Optional.empty();
            }
        }
    }
}
