package io.mitallast.maybe;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

import java.util.NoSuchElementException;
import java.util.function.Predicate;

public abstract class Maybe<T> implements Higher<Maybe, T> {
    private Maybe() {
    }

    abstract public <B> Maybe<B> flatMap(Function1<T, Maybe<B>> fn);

    abstract public <B> Maybe<B> map(Function1<T, B> fn);

    abstract public Maybe<T> filter(Predicate<T> predicate);

    abstract public T get();

    abstract public T getOrElse(T other);

    abstract public boolean isDefined();

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

    public static <T> Maybe<T> none() {
        return new Nothing<>();
    }

    public static class Just<T> extends Maybe<T> {
        private final T value;

        Just(T value) {
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
        public boolean isDefined() {
            return true;
        }

        @Override
        public String toString() {
            return "Just(" + value + ')';
        }
    }

    public static class Nothing<T> extends Maybe<T> {
        public Nothing() {
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
        public boolean isDefined() {
            return false;
        }

        @Override
        public String toString() {
            return "Nothing";
        }
    }
}
