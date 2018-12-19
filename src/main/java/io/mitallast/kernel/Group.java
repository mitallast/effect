package io.mitallast.kernel;

/**
 * A group is a monoid where each element has an inverse.
 */
public interface Group<A> extends Monoid<A> {
    A inverse(A a);

    default A remove(A a, A b) {
        return combine(a, inverse(b));
    }
}
