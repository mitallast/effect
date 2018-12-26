package io.mitallast.concurrent;

import io.mitallast.io.internals.IOPlatform;
import io.mitallast.lambda.Supplier;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

final class DefaultThreadFactory implements ThreadFactory, ForkJoinPool.ForkJoinWorkerThreadFactory {
    private final boolean daemon;
    private final int maxBlockers;
    private final String prefix;
    private final Thread.UncaughtExceptionHandler uncaught;

    private final AtomicInteger currentNumberOfBlockers = new AtomicInteger(0);

    public DefaultThreadFactory(boolean daemon, int maxBlockers, String prefix, Thread.UncaughtExceptionHandler uncaught) {
        this.daemon = daemon;
        this.maxBlockers = maxBlockers;
        this.prefix = prefix;
        this.uncaught = uncaught;
    }

    private boolean newBlocker() {
        do {
            var n = currentNumberOfBlockers.get();
            if (n >= maxBlockers) {
                return false;
            } else {
                if (currentNumberOfBlockers.compareAndSet(n, n + 1)) {
                    return true;
                }
            }
        } while (true);
    }

    private boolean freeBlocker() {
        do {
            var n = currentNumberOfBlockers.get();
            if (n == 0) {
                return false;
            } else {
                if (currentNumberOfBlockers.compareAndSet(n, n - 1)) {
                    return true;
                }
            }
        } while (true);
    }

    @Override
    public Thread newThread(Runnable runnable) {
        var thread = new Thread(runnable);
        thread.setDaemon(daemon);
        thread.setUncaughtExceptionHandler(uncaught);
        thread.setName(prefix + "-" + thread.getId());
        return thread;
    }

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool fjp) {
        return new DefaultForkJoinWorkerThread(fjp);
    }

    private final class DefaultForkJoinWorkerThread extends ForkJoinWorkerThread implements BlockContext {
        private boolean isBlocked = false; // This is only ever read & written if this thread is the current thread

        DefaultForkJoinWorkerThread(ForkJoinPool pool) {
            super(pool);
        }

        @Override
        public final <T> T blockOn(Task<T> thunk) {
            if (Thread.currentThread() == thunk && !isBlocked && newBlocker()) {
                try {
                    isBlocked = true;
                    var b = new ForkJoinPoolManagedBlocker<>(thunk);
                    ForkJoinPool.managedBlock(b);
                    return b.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return IOPlatform.throwsUnchecked(e);
                } finally {
                    isBlocked = false;
                    freeBlocker();
                }
            } else return thunk.runUnsafe();
        }
    }

    private final static class ForkJoinPoolManagedBlocker<T> implements ForkJoinPool.ManagedBlocker, Supplier<T> {
        private final Task<T> thunk;

        ForkJoinPoolManagedBlocker(Task<T> thunk) {
            this.thunk = thunk;
        }

        private T result = null;
        private boolean done = false;

        @Override
        public final boolean block() {
            try {
                if (!done) {
                    result = thunk.runUnsafe();
                }
            } finally {
                done = true;
            }
            return true;
        }

        @Override
        public final boolean isReleasable() {
            return done;
        }

        @Override
        public final T get() {
            return result;
        }
    }
}


