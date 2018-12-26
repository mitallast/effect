package io.mitallast.io.internals;

import io.mitallast.io.IO;
import io.mitallast.kernel.Unit;
import io.mitallast.list.List;

import java.util.Arrays;
import java.util.Iterator;

interface CancelUtils {
    static IO<Unit> cancelAll(IO<Unit>... cancelable) {
        if (cancelable.length == 0) {
            return IO.unit();
        } else {
            return IO.suspend(() -> cancelAll(Arrays.asList(cancelable).iterator()));
        }
    }

    static IO<Unit> cancelAll(List<IO<Unit>> cancelable) {
        if (cancelable.isEmpty()) {
            return IO.unit();
        } else {
            return IO.suspend(() -> cancelAll(cancelable.iterator()));
        }
    }

    static IO<Unit> cancelAll(Iterator<IO<Unit>> cursor) {
        if (!cursor.hasNext()) {
            return IO.unit();
        } else {
            return IO.suspend(() -> {
                var frame = new CancelAllFrame(cursor);
                return frame.loop();
            });
        }
    }
}

final class CancelAllFrame extends IOFrame<Unit, IO<Unit>> {
    private final Iterator<IO<Unit>> cursor;
    private volatile List<Throwable> errors = List.nil();

    CancelAllFrame(Iterator<IO<Unit>> cursor) {
        this.cursor = cursor;
    }

    IO<Unit> loop() {
        if (cursor.hasNext()) {
            return cursor.next().flatMap(this);
        } else {
            if (errors.isEmpty()) {
                return IO.unit();
            } else {
                return IO.raiseError(IOPlatform.composeErrors(errors.head(), errors.tail()));
            }
        }
    }

    @Override
    public IO<Unit> apply(Unit unit) {
        return loop();
    }

    @Override
    public IO<Unit> recover(Throwable e) {
        synchronized (cursor) {
            errors = errors.prepend(e);
        }
        return loop();
    }
}