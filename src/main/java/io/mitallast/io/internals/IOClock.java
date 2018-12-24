package io.mitallast.io.internals;

import io.mitallast.higher.Higher;
import io.mitallast.io.Clock;
import io.mitallast.io.IO;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

class IOClock implements Clock<IO> {
    @Override
    public Higher<IO, Long> realTime(TimeUnit unit) {
        return IO.pure(unit.convert(System.currentTimeMillis(), MILLISECONDS));
    }

    @Override
    public Higher<IO, Long> monotonic(TimeUnit unit) {
        return IO.pure(unit.convert(System.nanoTime(), NANOSECONDS));
    }
}
