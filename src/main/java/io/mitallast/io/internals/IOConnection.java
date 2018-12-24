package io.mitallast.io.internals;

import io.mitallast.io.IO;
import io.mitallast.kernel.Unit;
import io.mitallast.list.List;

import java.util.concurrent.atomic.AtomicReference;

/**
 * INTERNAL API — Represents a composite of functions
 * (meant for cancellation) that are stacked.
 * <p>
 * Implementation notes:
 * <p>
 * - `cancel()` is idempotent
 * - all methods are thread-safe / atomic
 * <p>
 * Used in the implementation of `cats.effect.IO`. Inspired by the
 * implementation of `StackedCancelable` from the Monix library.
 */
public abstract class IOConnection {
    private IOConnection() {
    }

    /**
     * Cancels the unit of work represented by this reference.
     * <p>
     * Guaranteed idempotency - calling it multiple times should have the
     * same side-effect as calling it only once. Implementations
     * of this method should also be thread-safe.
     */
    abstract public IO<Unit> cancel();

    /**
     * @return true in case this cancelable hasn't been canceled,
     * or false otherwise.
     */
    abstract public boolean isCanceled();

    /**
     * Pushes a cancelable reference on the stack, to be
     * popped or canceled later in FIFO order.
     */
    abstract public void push(IO<Unit> token);

    /**
     * Pushes a pair of `IOConnection` on the stack, which on
     * cancellation will get trampolined.
     * <p>
     * This is useful in `IO.race` for example, because combining
     * a whole collection of `IO` tasks, two by two, can lead to
     * building a cancelable that's stack unsafe.
     */
    abstract public void pushPair(IOConnection lh, IOConnection rh);

    /**
     * Removes a cancelable reference from the stack in FIFO order.
     *
     * @return the cancelable reference that was removed.
     */
    abstract public IO<Unit> pop();

    /**
     * Tries to reset an `IOConnection`, from a cancelled state,
     * back to a pristine state, but only if possible.
     * <p>
     * Returns `true` on success, or `false` if there was a race
     * condition (i.e. the connection wasn't cancelled) or if
     * the type of the connection cannot be reactivated.
     */
    abstract public boolean tryReactivate();

    /**
     * Builder for [[IOConnection]].
     */
    public static IOConnection apply() {
        return new Impl();
    }

    /**
     * Reusable [[IOConnection]] reference that cannot
     * be canceled.
     */
    public static IOConnection uncancelable() {
        return Uncancelable.instance;
    }

    private static final class Uncancelable extends IOConnection {
        public static final IOConnection instance = new Uncancelable();

        private Uncancelable() {
        }

        @Override
        public IO<Unit> cancel() {
            return IO.unit();
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public void push(IO<Unit> token) {
        }

        @Override
        public void pushPair(IOConnection lh, IOConnection rh) {

        }

        @Override
        public IO<Unit> pop() {
            return IO.unit();
        }

        @Override
        public boolean tryReactivate() {
            return true;
        }
    }

    private static final class Impl extends IOConnection {
        private final AtomicReference<List<IO<Unit>>> state = new AtomicReference<>(List.empty());


        @Override
        public IO<Unit> cancel() {
            var list = state.getAndSet(null);
            if (list == null || list.isEmpty()) {
                return IO.unit();
            } else {
                return CancelUtils.cancelAll(list);
            }
        }

        @Override
        public boolean isCanceled() {
            return state.get() == null;
        }

        @Override
        public void push(IO<Unit> cancelable) {
            while (true) {
                var list = state.get();
                if (list == null) {
                    cancelable.unsafeRunAsyncAndForget();
                    break;
                } else {
                    var update = list.prepend(cancelable);
                    if (state.compareAndSet(list, update)) {
                        break;
                    }
                }
            }
        }

        @Override
        public void pushPair(IOConnection lh, IOConnection rh) {
            push(CancelUtils.cancelAll(lh.cancel(), rh.cancel()));
        }

        @Override
        public IO<Unit> pop() {
            while (true) {
                var list = state.get();
                if (list == null || list.isEmpty()) {
                    return IO.unit();
                } else {
                    if (state.compareAndSet(list, list.tail())) {
                        return list.head();
                    }
                }
            }
        }

        @Override
        public boolean tryReactivate() {
            return state.compareAndSet(null, List.empty());
        }
    }
}
