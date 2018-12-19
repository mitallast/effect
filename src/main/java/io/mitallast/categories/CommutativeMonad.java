package io.mitallast.categories;

import io.mitallast.higher.Higher;

public interface CommutativeMonad<F extends Higher> extends Monad<F>, CommutativeFlatMap<F>, CommutativeApplicative<F> {
}
