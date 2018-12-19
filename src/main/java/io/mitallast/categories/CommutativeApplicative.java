package io.mitallast.categories;

import io.mitallast.higher.Higher;

public interface CommutativeApplicative<F extends Higher> extends Applicative<F>, CommutativeApply<F> {
}
