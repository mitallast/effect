package io.mitallast.io;

import io.mitallast.higher.Higher;
import io.mitallast.kernel.Unit;

import java.time.Duration;

public interface Timer<F extends Higher> {

    Clock<F> clock();

    Higher<F, Unit> sleep(Duration duration);
}
