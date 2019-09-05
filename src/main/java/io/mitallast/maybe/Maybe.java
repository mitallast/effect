package io.mitallast.maybe;

import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Supplier;
import io.mitallast.list.List;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class Maybe<T> implements Higher<Maybe, T> {
    private Maybe() {
    }

    abstract public <B> Maybe<B> flatMap(Function1<T, Maybe<B>> fn);

    abstract public <B> Maybe<B> map(Function1<T, B> fn);

    abstract public void foreach(Consumer<T> fn);

    abstract public Maybe<T> filter(Predicate<T> predicate);

    abstract public T get();

    abstract public T getOrElse(T other);

    abstract public Maybe<T> orElse(Maybe<T> other);

    abstract public boolean isDefined();

    abstract public <B> B fold(Supplier<B> ifEmpty, Function1<T, B> f);

    abstract public <X> Either<T, X> toLeft(Supplier<X> right);

    abstract public <X> Either<X, T> toRight(Supplier<X> right);

    abstract public List<T> toList();

    public boolean nonEmpty() {
        return isDefined();
    }

    public boolean isEmpty() {
        return !isDefined();
    }


    public static <T> Maybe<T> apply(T value) {
        if (value == null) {
            return none();
        } else {
            return new Just<>(value);
        }
    }

    public static <T> Maybe<T> some(T value) {
        return new Just<>(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> Maybe<T> none() {
        return (Nothing<T>) Nothing.instance;
    }

    public static class Just<T> extends Maybe<T> {
        private final T value;

        private Just(T value) {
            this.value = value;
        }

        public T value() {
            return value;
        }

        @Override
        public <B> Maybe<B> flatMap(Function1<T, Maybe<B>> fn) {
            return fn.apply(value);
        }

        @Override
        public <B> Maybe<B> map(Function1<T, B> fn) {
            return new Just<>(fn.apply(value));
        }

        @Override
        public void foreach(final Consumer<T> fn) {
            fn.accept(value);
        }

        @Override
        public Maybe<T> filter(Predicate<T> predicate) {
            if (predicate.test(value)) {
                return this;
            } else {
                return none();
            }
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public T getOrElse(T other) {
            return value;
        }

        @Override
        public Maybe<T> orElse(final Maybe<T> other) {
            return this;
        }

        @Override
        public boolean isDefined() {
            return true;
        }

        @Override
        public <B> B fold(final Supplier<B> ifEmpty, final Function1<T, B> f) {
            return f.apply(value);
        }

        @Override
        public <X> Either<T, X> toLeft(final Supplier<X> right) {
            return Either.left(value);
        }

        @Override
        public <X> Either<X, T> toRight(final Supplier<X> right) {
            return Either.right(value);
        }

        @Override
        public List<T> toList() {
            return List.of(value);
        }

        @Override
        public String toString() {
            return "Just(" + value + ')';
        }
    }

    public static class Nothing<T> extends Maybe<T> {
        private static Nothing instance = new Nothing();

        private Nothing() {
        }

        @Override
        public <B> Maybe<B> flatMap(Function1<T, Maybe<B>> fn) {
            return none();
        }

        @Override
        public <B> Maybe<B> map(Function1<T, B> fn) {
            return none();
        }

        @Override
        public void foreach(final Consumer<T> fn) {
        }

        @Override
        public Maybe<T> filter(Predicate<T> predicate) {
            return this;
        }

        @Override
        public T get() {
            throw new NoSuchElementException("Nothing.get");
        }

        @Override
        public T getOrElse(T other) {
            return other;
        }

        @Override
        public Maybe<T> orElse(final Maybe<T> other) {
            return other;
        }

        @Override
        public boolean isDefined() {
            return false;
        }

        @Override
        public <B> B fold(final Supplier<B> ifEmpty, final Function1<T, B> f) {
            return ifEmpty.get();
        }

        @Override
        public <X> Either<T, X> toLeft(final Supplier<X> right) {
            return Either.right(right.get());
        }

        @Override
        public <X> Either<X, T> toRight(final Supplier<X> left) {
            return Either.left(left.get());
        }

        @Override
        public List<T> toList() {
            return List.empty();
        }

        @Override
        public String toString() {
            return "Nothing";
        }
    }
}
