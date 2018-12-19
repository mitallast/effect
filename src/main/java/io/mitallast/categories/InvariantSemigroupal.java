package io.mitallast.categories;

import io.mitallast.higher.Higher;

/**
 * [[InvariantSemigroupal]] is nothing more than something both invariant
 * and Semigroupal. It comes up enough to be useful, and composes well
 */
public interface InvariantSemigroupal<F extends Higher> extends Invariant<F>, Semigroupal<F> {
}
