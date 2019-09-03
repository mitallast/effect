package io.mitallast.stream;

import io.mitallast.data.NonEmptyList;
import io.mitallast.list.List;
import io.mitallast.maybe.Maybe;

public final class CompositeFailure extends Throwable {
    private final Throwable head;
    private final NonEmptyList<Throwable> tail;

    private CompositeFailure(final Throwable head, final NonEmptyList<Throwable> tail) {
        this.head = head;
        this.tail = tail;
    }

    public static Throwable apply(Throwable first, Throwable second) {
        return new CompositeFailure(first, NonEmptyList.of(second));
    }

    public static Maybe<Throwable> fromList(List<Throwable> errors) {
        if (errors.isEmpty()) return Maybe.none();
        var head = errors.head();
        var tail = errors.tail();
        if (tail.isEmpty()) {
            return Maybe.some(head);
        } else {
            return Maybe.some(new CompositeFailure(head, NonEmptyList.of(tail.head(), tail.tail())));
        }
    }
}
