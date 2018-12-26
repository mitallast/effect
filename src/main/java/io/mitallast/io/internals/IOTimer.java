package io.mitallast.io.internals;

import io.mitallast.concurrent.ExecutionContext;
import io.mitallast.either.Either;
import io.mitallast.io.*;
import io.mitallast.kernel.Unit;

import java.time.Duration;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

/**
 * Internal API — JVM specific implementation of a `Timer[IO]`.
 * <p>
 * Depends on having a Scala `ExecutionContext` for the
 * execution of tasks after their schedule (i.e. bind continuations) and on a Java
 * `ScheduledExecutorService` for scheduling ticks with a delay.
 */
public final class IOTimer implements Timer<IO> {
    private final ExecutionContext ec;
    private final ScheduledExecutorService sc;

    private IOTimer(ExecutionContext ec, ScheduledExecutorService sc) {
        this.ec = ec;
        this.sc = sc;
    }

    @Override
    public Clock<IO> clock() {
        return Clock.create(IO.effect());
    }

    @Override
    public IO<Unit> sleep(Duration duration) {
        return new IO.Async<>((conn, cb) -> {
            // Doing what IO.cancelable does
            var ref = ForwardCancelable.apply();
            conn.push(ref.cancel());
            var f = sc.schedule(new ShiftTick(conn, cb, ec), duration.toMillis(), MILLISECONDS);
            ref.set(IO.delay(() -> {
                f.cancel(false);
                return Unit.unit();
            }));
        });
    }

    public static Timer<IO> apply(ExecutionContext ec) {
        return apply(ec, scheduler);
    }

    public static Timer<IO> apply(ExecutionContext ec, ScheduledExecutorService scheduler) {
        return new IOTimer(ec, scheduler);
    }

    private final static ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(2, r -> {
            var th = new Thread(r);
            th.setName("effect-scheduler-" + th.getId());
            th.setDaemon(true);
            return th;
        });

    private final static class ShiftTick implements Runnable {
        private final IOConnection conn;
        private final Consumer<Either<Throwable, Unit>> cb;
        private final ExecutionContext ec;

        private ShiftTick(IOConnection conn, Consumer<Either<Throwable, Unit>> cb, ExecutionContext ec) {
            this.conn = conn;
            this.cb = cb;
            this.ec = ec;
        }

        @Override
        public void run() {
            // Shifts actual execution on our `ExecutionContext`, because
            // the scheduler is in charge only of ticks and the execution
            // needs to shift because the tick might continue with whatever
            // bind continuation is linked to it, keeping the current thread
            // occupied
            conn.pop();
            ec.execute(new Tick(cb));
        }
    }
}
