package io.mitallast.stream;

import io.mitallast.higher.Higher;
import io.mitallast.io.Concurrent;
import io.mitallast.maybe.Maybe;

interface TranslateInterrupt<F extends Higher> {
    Maybe<Concurrent<F>> concurrentInstance();
}
