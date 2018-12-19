package io.mitallast.kernel;

/**
 * CommutativeMonoid represents a commutative monoid.
 *
 * A monoid is commutative if for all x and y, x |+| y === y |+| x.
 */
public interface CommutativeMonoid<A> extends CommutativeSemigroup<A>, Monoid<A> {
}
