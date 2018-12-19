package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

public interface Invariant<F extends Higher> {
    <A, B> Higher<F, B> imap(Higher<F, A> fa, Function1<A, B> f, Function1<B, A> g);
}
