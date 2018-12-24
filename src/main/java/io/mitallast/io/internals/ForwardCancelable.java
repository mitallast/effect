package io.mitallast.io.internals;

import io.mitallast.io.IO;
import io.mitallast.kernel.Unit;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class ForwardCancelable {
    private final IO<Unit> plusOne;
    private final AtomicReference<State> state = new AtomicReference<>(IsEmpty.instance);

    ForwardCancelable(IO<Unit> plusOne) {
        this.plusOne = plusOne;
    }

    private IO<Unit> loop(CompletableFuture<Unit> ref) {
        var s = state.get();
        if (s instanceof IsCanceled) {
            return IO.unit();
        } else if (s instanceof IsEmptyCanceled) {
            var promise = ((IsEmptyCanceled) s).promise;
            return IO.fromFuture(promise);
        } else if (s instanceof IsEmpty) {
            var p = ref != null ? ref : new CompletableFuture<Unit>();
            if (!state.compareAndSet(s, new IsEmptyCanceled(p))) {
                return loop(p); // retry
            } else {
                var token = IO.fromFuture(p);
                if (plusOne != null) {
                    return CancelUtils.cancelAll(token, plusOne);
                } else {
                    return token;
                }
            }
        } else if (s instanceof Reference) {
            var token = ((Reference) s).token;
            if (!state.compareAndSet(s, IsCanceled.instance)) {
                return loop(ref); // retry
            } else if (plusOne != null) {
                return CancelUtils.cancelAll(token, plusOne);
            } else {
                return token;
            }
        } else throw new IllegalStateException();
    }

    public IO<Unit> cancel() {
        return IO.suspend(() -> loop(null));
    }

    public void set(IO<Unit> token) {
        var s = state.get();
        if (s instanceof IsEmpty) {
            if (!state.compareAndSet(s, new Reference(token))) {
                set(token); // retry;
            }
        } else if (s instanceof IsEmptyCanceled) {
            var p = ((IsEmptyCanceled) s).promise;
            if (!state.compareAndSet(s, IsCanceled.instance)) {
                set(token); // retry
            } else {
                token.unsafeRunAsync(Callback.promise(p));
            }
        } else throw new IllegalStateException();
    }

    public abstract static class State {
    }

    public final static class IsEmpty extends State {
        public static final IsEmpty instance = new IsEmpty();

        private IsEmpty() {
        }
    }

    public final static class IsCanceled extends State {
        public static final IsCanceled instance = new IsCanceled();

        private IsCanceled() {
        }
    }

    public final static class IsEmptyCanceled extends State {
        private final CompletableFuture<Unit> promise;

        IsEmptyCanceled(CompletableFuture<Unit> promise) {
            this.promise = promise;
        }
    }

    public final static class Reference extends State {
        private final IO<Unit> token;

        Reference(IO<Unit> token) {
            this.token = token;
        }
    }

    public static ForwardCancelable apply() {
        return new ForwardCancelable(null);
    }
}
