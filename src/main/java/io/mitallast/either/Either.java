package io.mitallast.either;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface Either<A, B> {

    static <A, B> Left<A, B> left(A value) {
        return new Left<>(value);
    }

    static <A, B> Right<A, B> right(B value) {
        return new Right<>(value);
    }

    boolean isLeft();

    boolean isRight();

    LeftProjection<A, B> left();

    RightProjection<A, B> right();

    <C> C fold(Function<A, C> fa, Function<B, C> fb);

    Either<B, A> swap();

    void foreach(Consumer<B> f);

    B getOrElse(Supplier<B> or);

    boolean contains(B elem);

    boolean forall(Predicate<B> f);

    boolean exists(Predicate<B> f);

    <C> Either<A, C> flatMap(Function<B, Either<A, C>> f);

    <C> Either<A, C> map(Function<B, C> f);

    Either<A, B> filterOrElse(Predicate<B> p, Supplier<A> zero);
}

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

interface RightProjection<A, B> {
    B get();

    void foreach(Consumer<B> f);

    B getOrElse(Supplier<B> or);

    boolean forall(Predicate<B> f);

    boolean exists(Predicate<B> f);

    Either<A, B> flatMap(Function<B, Either<A, B>> f);

    Either<A, B> map(Function<B, B> f);

    Optional<Either<A, B>> filter(Predicate<B> f);
}
