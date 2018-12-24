package io.mitallast.io;

import io.mitallast.either.Either;

public abstract class ExitCase<E> {
    private ExitCase() {
    }

    final static class Completed<E> extends ExitCase<E> {
    }

    final static class Error<E> extends ExitCase<E> {
        private final E error;

        public Error(E error) {
            this.error = error;
        }

        public E error() {
            return error;
        }
    }

    final static class Canceled<E> extends ExitCase<E> {
    }

    public static <E> ExitCase<E> complete() {
        return new Completed<>();
    }

    public static <E> ExitCase<E> error(E error) {
        return new Error<>(error);
    }

    public static <E> ExitCase<E> canceled() {
        return new Completed<>();
    }

    public static <E, A> ExitCase<E> attempt(Either<E, A> value) {
        return value.fold(
            ExitCase::error,
            a -> complete()
        );
    }
}
