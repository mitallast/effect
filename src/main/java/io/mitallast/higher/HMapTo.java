package io.mitallast.higher;

import io.mitallast.lambda.Function1;

public interface HMapTo<F extends Higher, A, B> extends Function1<A, Higher<F, B>> {
    @Override
    default Higher<F, B> apply(A a) {
        return map(a);
    }

    <FB extends Higher<F, B>> FB map(A a);
}
