package io.mitallast.io;

import io.mitallast.higher.Higher;

import java.util.concurrent.TimeUnit;

public interface Clock<F extends Higher> {
    /**
     * Returns the current time, as a Unix timestamp (number of time units
     * since the Unix epoch), suspended in `F[_]`.
     * <p>
     * This is the pure equivalent of Java's `System.currentTimeMillis`,
     * or of `CLOCK_REALTIME` from Linux's `clock_gettime()`.
     * <p>
     * The provided `TimeUnit` determines the time unit of the output,
     * its precision, but not necessarily its resolution, which is
     * implementation dependent. For example this will return the number
     * of milliseconds since the epoch:
     * <p>
     * {{{
     * import scala.concurrent.duration.MILLISECONDS
     * <p>
     * clock.realTime(MILLISECONDS)
     * }}}
     * <p>
     * N.B. the resolution is limited by the underlying implementation
     * and by the underlying CPU and OS. If the implementation uses
     * `System.currentTimeMillis`, then it can't have a better
     * resolution than 1 millisecond, plus depending on underlying
     * runtime (e.g. Node.js) it might return multiples of 10
     * milliseconds or more.
     * <p>
     * See [[monotonic]], for fetching a monotonic value that
     * may be better suited for doing time measurements.
     */
    Higher<F, Long> realTime(TimeUnit unit);

    /**
     * Returns a monotonic clock measurement, if supported by the
     * underlying platform.
     * <p>
     * This is the pure equivalent of Java's `System.nanoTime`,
     * or of `CLOCK_MONOTONIC` from Linux's `clock_gettime()`.
     * <p>
     * {{{
     * clock.monotonic(NANOSECONDS)
     * }}}
     * <p>
     * The returned value can have nanoseconds resolution and represents
     * the number of time units elapsed since some fixed but arbitrary
     * origin time. Usually this is the Unix epoch, but that's not
     * a guarantee, as due to the limits of `Long` this will overflow in
     * the future (2^63^ is about 292 years in nanoseconds) and the
     * implementation reserves the right to change the origin.
     * <p>
     * The return value should not be considered related to wall-clock
     * time, the primary use-case being to take time measurements and
     * compute differences between such values, for example in order to
     * measure the time it took to execute a task.
     * <p>
     * As a matter of implementation detail, the default `Clock[IO]`
     * implementation uses `System.nanoTime` and the JVM will use
     * `CLOCK_MONOTONIC` when available, instead of `CLOCK_REALTIME`
     * (see `clock_gettime()` on Linux) and it is up to the underlying
     * platform to implement it correctly.
     * <p>
     * And be warned, there are platforms that don't have a correct
     * implementation of `CLOCK_MONOTONIC`. For example at the moment of
     * writing there is no standard way for such a clock on top of
     * JavaScript and the situation isn't so clear cut for the JVM
     * either, see:
     * <p>
     * - [[https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6458294 bug report]]
     * - [[http://cs.oswego.edu/pipermail/concurrency-interest/2012-January/008793.html concurrency-interest]]
     * discussion on the X86 tsc register
     * <p>
     * The JVM tries to do the right thing and at worst the resolution
     * and behavior will be that of `System.currentTimeMillis`.
     * <p>
     * The recommendation is to use this monotonic clock when doing
     * measurements of execution time, or if you value monotonically
     * increasing values more than a correspondence to wall-time, or
     * otherwise prefer [[realTime]].
     */
    Higher<F, Long> monotonic(TimeUnit unit);

    static <F extends Higher> Clock<F> create(Sync<F> F) {
        return new Clock<F>() {
            @Override
            public Higher<F, Long> realTime(TimeUnit unit) {
                return F.delay(() -> unit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS));
            }

            @Override
            public Higher<F, Long> monotonic(TimeUnit unit) {
                return F.delay(() -> unit.convert(System.nanoTime(), TimeUnit.NANOSECONDS));
            }
        };
    }
}
