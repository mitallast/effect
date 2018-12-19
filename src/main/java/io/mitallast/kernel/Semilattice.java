package io.mitallast.kernel;

/**
 * Semilattices are commutative semigroups whose operation
 * (i.e. combine) is also idempotent.
 */
public interface Semilattice<A> extends Band<A>, CommutativeSemigroup<A> {
}
