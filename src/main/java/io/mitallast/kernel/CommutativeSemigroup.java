package io.mitallast.kernel;

/**
 * CommutativeSemigroup represents a commutative semigroup.
 *
 * A semigroup is commutative if for all x and y, x |+| y === y |+| x.
 */
public interface CommutativeSemigroup<A> extends Semigroup<A> {
}
