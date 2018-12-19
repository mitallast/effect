package io.mitallast.categories;

import io.mitallast.higher.Higher;

public interface CommutativeFlatMap<F extends Higher> extends FlatMap<F>, CommutativeApply<F> {
}
