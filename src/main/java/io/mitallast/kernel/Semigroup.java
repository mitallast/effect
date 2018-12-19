package io.mitallast.kernel;

/**
 * A semigroup is any set `A` with an associative operation (`combine`).
 */
public interface Semigroup<A> {
    A combine(A x, A y);
}
