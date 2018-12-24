package io.mitallast.io.internals;

import io.mitallast.either.Either;
import io.mitallast.io.ContextShift;
import io.mitallast.io.IO;
import io.mitallast.kernel.Unit;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

abstract class IOForkedStart<A> implements BiConsumer<IOConnection, Consumer<Either<Throwable, A>>> {
    /**
     * Given a task, returns one that has a guaranteed
     * logical fork on execution.
     * <p>
     * The equivalent of `IO.shift *> task` but it tries
     * to eliminate extraneous async boundaries. In case the
     * task is known to fork already, then this introduces
     * a light async boundary instead.
     */
    static <A> IO<A> apply(IO<A> task, ContextShift<IO> cs) {
        if (detect(task)) return task;
        else return ((IO<Unit>) cs.shift()).flatMap(u -> task);
    }

    private static boolean detect(IO task) {
        return detect(task, 8);
    }

    /**
     * Returns `true` if the given task is known to fork execution,
     * or `false` otherwise.
     */
    private static boolean detect(IO task, int limit) {
        if (limit > 0) {
            if (task instanceof IO.Async) {
                return ((IO.Async) task).k() instanceof IOForkedStart;
            } else if (task instanceof IO.Bind) {
                return detect(((IO.Bind) task).source(), limit - 1);
            } else if (task instanceof IO.Map) {
                return detect(((IO.Map) task).source(), limit - 1);
            } else if (task instanceof IO.ContextSwitch) {
                return detect(((IO.ContextSwitch) task).source(), limit - 1);
            } else return false;
        } else {
            return false;
        }
    }
}
