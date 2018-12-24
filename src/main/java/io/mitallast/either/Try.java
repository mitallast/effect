package io.mitallast.either;

import io.mitallast.higher.Higher;
import io.mitallast.maybe.Maybe;

import java.util.function.Supplier;

public abstract class Try<A> implements Higher<Try, A> {
    private Try() {
    }

    abstract public Maybe<A> toOption();

    public static <A> Try<A> apply(Supplier<A> thunk) {
        try {
            A value = thunk.get();
            return new Success<>(value);
        } catch (Exception e) {
            return new Failure<>(e);
        }
    }

    public static class Success<A> extends Try<A> {
        private final A value;

        public Success(A value) {
            this.value = value;
        }

        public A value() {
            return value;
        }

        @Override
        public Maybe<A> toOption() {
            return Maybe.apply(value);
        }
    }

    public static class Failure<A> extends Try<A> {
        private final Throwable error;

        public Failure(Throwable error) {
            this.error = error;
        }

        public Throwable error() {
            return error;
        }

        @Override
        public Maybe<A> toOption() {
            return Maybe.none();
        }
    }
}
