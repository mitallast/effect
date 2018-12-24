package io.mitallast.io;

import io.mitallast.higher.Higher;

public interface LiftIO<F extends Higher> {
    <A> Higher<F, A> liftIO(IO<A> ioa);
}
