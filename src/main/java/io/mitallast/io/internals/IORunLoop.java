package io.mitallast.io.internals;

import io.mitallast.either.Either;
import io.mitallast.io.IO;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Function4;

import java.util.function.Consumer;

public interface IORunLoop {
    static <A> void start(IO<A> source, Consumer<Either<Throwable, A>> cb) {
        loop(source, IOConnection.uncancelable(), cb, null, null, null);
    }

    static <A> void startCancelable(IO<A> source, IOConnection conn, Consumer<Either<Throwable, A>> cb) {
        loop(source, conn, cb, null, null, null);
    }

    /**
     * Loop for evaluating an `IO` value.
     * <p>
     * The `rcbRef`, `bFirstRef` and `bRestRef`  parameters are
     * nullable values that can be supplied because the loop needs
     * to be resumed in [[RestartCallback]].
     */
    static <A> void loop(
        final IO<A> source,
        final IOConnection cancelable,
        final Consumer<Either<Throwable, A>> cb,
        final RestartCallback rcbRef,
        final Function1<Object, IO<Object>> bFirstRef,
        final ArrayStack<Function1<Object, IO<Object>>> bRestRef) {

        IO currentIO = source;
        // Can change on a context switch
        IOConnection conn = cancelable;
        Function1<Object, IO<Object>> bFirst = bFirstRef;
        ArrayStack<Function1<Object, IO<Object>>> bRest = bRestRef;
        RestartCallback rcb = rcbRef;
        // Values from Pure and Delay are unboxed in this var,
        // for code reuse between Pure and Delay
        boolean hasUnboxed = false;
        Object unboxed = null;

        do {
            if (currentIO instanceof IO.Bind) {
                var fa = ((IO.Bind) currentIO).source();
                var bindNext = ((IO.Bind) currentIO).f();
                if (bFirst != null) {
                    if (bRest == null) bRest = new ArrayStack<>();
                    bRest.push(bFirst);
                }
                bFirst = bindNext;
                currentIO = fa;
            } else if (currentIO instanceof IO.Pure) {
                unboxed = ((IO.Pure) currentIO).a();
                hasUnboxed = true;
            } else if (currentIO instanceof IO.Delay) {
                try {
                    unboxed = ((IO.Delay) currentIO).thunk().get();
                    hasUnboxed = true;
                    currentIO = null;
                } catch (Exception e) {
                    currentIO = IO.raiseError(e);
                }
            } else if (currentIO instanceof IO.Suspend) {
                try {
                    currentIO = (IO) ((IO.Suspend) currentIO).thunk().get();
                } catch (Exception e) {
                    currentIO = IO.raiseError(e);
                }
            } else if (currentIO instanceof IO.RaiseError) {
                var bind = findErrorHandler(bFirst, bRest);
                if (bind == null) {
                    cb.accept(Either.left(((IO.RaiseError) currentIO).e()));
                    return;
                } else {
                    IO fa;
                    try {
                        fa = bind.recover(((IO.RaiseError) currentIO).e());
                    } catch (Exception e) {
                        fa = IO.raiseError(e);
                    }
                    bFirst = null;
                    currentIO = fa;
                }
            } else if (currentIO instanceof IO.Map) {
                var fa = ((IO.Map) currentIO).source();
                var bindNext = (IO.Map) currentIO;
                if (bFirst != null) {
                    if (bRest == null) bRest = new ArrayStack<>();
                    bRest.push(bFirst);
                }
                bFirst = bindNext;
                currentIO = fa;
            } else if (currentIO instanceof IO.Async) {
                if (conn == null) conn = IOConnection.apply();
                if (rcb == null) rcb = new RestartCallback<>(conn, cb);
                rcb.start((IO.Async) currentIO, bFirst, bRest);
                return;
            } else if (currentIO instanceof IO.ContextSwitch) {
                var next = ((IO.ContextSwitch) currentIO).source();
                Function1<IOConnection, IOConnection> modify = ((IO.ContextSwitch) currentIO).modify();
                var restore = ((IO.ContextSwitch) currentIO).restore();

                IOConnection old = (conn != null) ? conn : IOConnection.apply();
                conn = modify.apply(old);
                currentIO = next;
                if (conn != old) {
                    if (rcb != null) rcb.contextSwitch(conn);
                    if (restore != null)
                        currentIO = new IO.Bind(next, new RestoreContext(old, restore));
                }
            } else throw new IllegalArgumentException("");

            if (hasUnboxed) {
                var bind = popNextBind(bFirst, bRest);
                if (bind == null) {
                    cb.accept(Either.right((A) unboxed));
                    return;
                } else {
                    IO fa;
                    try {
                        fa = bind.apply(unboxed);
                    } catch (Exception e) {
                        fa = IO.raiseError(e);
                    }
                    hasUnboxed = false;
                    unboxed = null;
                    bFirst = null;
                    currentIO = fa;
                }
            }
        } while (true);
    }

    static <A> IO<A> step(final IO<A> source) {
        IO currentIO = source;
        Function1<Object, IO<Object>> bFirst = null;
        ArrayStack<Function1<Object, IO<Object>>> bRest = null;
        // Values from Pure and Delay are unboxed in this var,
        // for code reuse between Pure and Delay
        boolean hasUnboxed = false;
        Object unboxed = null;

        do {
            if (currentIO instanceof IO.Bind) {
                if (bFirst != null) {
                    if (bRest == null) bRest = new ArrayStack<>();
                    bRest.push(bFirst);
                }
                bFirst = ((IO.Bind) currentIO).f();
                currentIO = ((IO.Bind) currentIO).source();
            } else if (currentIO instanceof IO.Pure) {
                unboxed = ((IO.Pure) currentIO).a();
                hasUnboxed = true;
            } else if (currentIO instanceof IO.Delay) {
                try {
                    unboxed = ((IO.Delay) currentIO).thunk().get();
                    hasUnboxed = true;
                    currentIO = null;
                } catch (Exception e) {
                    currentIO = IO.raiseError(e);
                }
            } else if (currentIO instanceof IO.Suspend) {
                try {
                    currentIO = (IO) ((IO.Suspend) currentIO).thunk().get();
                } catch (Exception e) {
                    currentIO = IO.raiseError(e);
                }
            } else if (currentIO instanceof IO.RaiseError) {
                var bind = findErrorHandler(bFirst, bRest);
                if (bind == null) {
                    return currentIO;
                } else {
                    IO fa;
                    try {
                        fa = bind.recover(((IO.RaiseError) currentIO).e());
                    } catch (Exception e) {
                        fa = IO.raiseError(e);
                    }
                    bFirst = null;
                    currentIO = fa;
                }
            } else if (currentIO instanceof IO.Map) {
                var fa = ((IO.Map) currentIO).source();
                var bindNext = (IO.Map) currentIO;
                if (bFirst != null) {
                    if (bRest == null) bRest = new ArrayStack<>();
                    bRest.push(bFirst);
                }
                bFirst = bindNext;
                currentIO = fa;
            } else {
                return suspendInAsync(currentIO, bFirst, bRest);
            }

            if (hasUnboxed) {
                var bind = popNextBind(bFirst, bRest);
                if (bind == null) {
                    if (currentIO != null) {
                        return currentIO;
                    } else {
                        return IO.pure((A) unboxed);
                    }
                } else {
                    IO fa;
                    try {
                        fa = bind.apply(unboxed);
                    } catch (Exception e) {
                        fa = IO.raiseError(e);
                    }
                    hasUnboxed = false;
                    unboxed = null;
                    bFirst = null;
                    currentIO = fa;
                }
            }
        } while (true);
    }

    private static <A> IO<A> suspendInAsync(IO<A> currentIO,
                                            Function1<Object, IO<Object>> bFirst,
                                            ArrayStack<Function1<Object, IO<Object>>> bRest) {
        // Hitting an async boundary means we have to stop, however
        // if we had previous `flatMap` operations then we need to resume
        // the loop with the collected stack
        if (bFirst != null || (bRest != null && !bRest.isEmpty())) {
            return new IO.Async<>((conn, cb) -> loop(currentIO, conn, cb, null, bFirst, bRest));
        } else {
            return currentIO;
        }
    }

    /**
     * Pops the next bind function from the stack, but filters out
     * `IOFrame.ErrorHandler` references, because we know they won't do
     * anything — an optimization for `handleError`.
     */
    private static Function1<Object, IO<Object>> popNextBind(Function1<Object, IO<Object>> bFirst,
                                                             ArrayStack<Function1<Object, IO<Object>>> bRest) {
        if ((bFirst != null) && !(bFirst instanceof ErrorHandler))
            return bFirst;

        if (bRest == null) return null;
        do {
            var next = bRest.pop();
            if (next == null) {
                return null;
            } else if (!(next instanceof ErrorHandler)) {
                return next;
            }
        } while (true);
    }

    /**
     * Finds a [[IOFrame]] capable of handling errors in our bind
     * call-stack, invoked after a `RaiseError` is observed.
     */
    private static IOFrame<Object, IO<Object>> findErrorHandler(Function1<Object, IO<Object>> bFirst,
                                                                ArrayStack<Function1<Object, IO<Object>>> bRest) {
        if (bFirst instanceof IOFrame) {
            return (IOFrame<Object, IO<Object>>) bFirst;
        } else {
            if (bRest == null) return null;
            else {
                do {
                    var ref = bRest.pop();
                    if (ref == null) {
                        return null;
                    } else if (ref instanceof IOFrame) {
                        return (IOFrame<Object, IO<Object>>) ref;
                    }
                } while (true);
            }
        }
    }
}

@SuppressWarnings("unchecked")
final class RestartCallback<A> implements Consumer<Either<Throwable, A>>, Runnable {
    private final Consumer<Either<Throwable, A>> cb;

    // can change on a ContextSwitch
    private IOConnection conn;
    private boolean canCall = false;
    private boolean trampolineAfter = false;
    private Function1<Object, IO<Object>> bFirst = null;
    private ArrayStack<Function1<Object, IO<Object>>> bRest = null;

    // Used in combination with trampolineAfter = true
    private Either<Throwable, A> value = null;

    RestartCallback(IOConnection connInit, Consumer<Either<Throwable, A>> cb) {
        this.conn = connInit;
        this.cb = cb;
    }

    void contextSwitch(IOConnection conn) {
        this.conn = conn;
    }

    void start(IO.Async task,
               Function1<Object, IO<Object>> bFirst,
               ArrayStack<Function1<Object, IO<Object>>> bRest) {
        canCall = true;
        this.bFirst = bFirst;
        this.bRest = bRest;
        this.trampolineAfter = task.trampolineAfter();
        // Go, go, go
        task.k().accept(conn, this);
    }

    private void signal(Either<Throwable, A> either) {
        // Auto-cancelable logic: in case the connection was cancelled,
        // we interrupt the bind continuation
        if (!conn.isCanceled()) {
            either.foreach(
                err -> IORunLoop.loop(IO.raiseError(err), conn, cb, this, bFirst, bRest),
                success -> IORunLoop.loop(IO.pure(success), conn, cb, this, bFirst, bRest)
            );
        }
    }

    @Override
    public void run() {
        // N.B. this has to be set to null *before* the signal
        // otherwise a race condition can happen ;-)
        var v = value;
        value = null;
        signal(v);
    }

    @Override
    public void accept(Either<Throwable, A> either) {
        if (canCall) {
            canCall = false;
            if (trampolineAfter) {
                this.value = either;
                TrampolineEC.immediate.execute(this);
            } else {
                signal(either);
            }
        }
    }
}

final class RestoreContext<A> extends IOFrame<A, IO<A>> {
    private final IOConnection old;
    private final Function4<A, Throwable, IOConnection, IOConnection, IOConnection> restore;

    RestoreContext(IOConnection old, Function4<A, Throwable, IOConnection, IOConnection, IOConnection> restore) {
        this.old = old;
        this.restore = restore;
    }

    @Override
    public IO<A> apply(A a) {
        return new IO.ContextSwitch<>(IO.pure(a), current -> restore.apply(a, null, old, current), null);
    }

    @Override
    public IO<A> recover(Throwable e) {
        return new IO.ContextSwitch<>(IO.raiseError(e), current -> restore.apply(null, e, old, current), null);
    }
}
