package io.mitallast.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

public class DefaultExecutionContext implements ExecutionContextExecutor {
    private final Executor executor;
    private final Consumer<Throwable> reporter;

    private static ExecutorService createDefaultExecutorService(Consumer<Throwable> reporter) {
        var handler = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                reporter.accept(e);
            }
        };
        var threadFactory = new DefaultThreadFactory(true, 256, "global-ec", handler);
        return new ForkJoinPool(1, threadFactory, handler, true);
    }

    static DefaultExecutionContext fromExecutor(Executor executor, Consumer<Throwable> reporter) {
        if (executor == null) {
            executor = createDefaultExecutorService(reporter);
        }
        return new DefaultExecutionContext(executor, reporter);
    }

    private DefaultExecutionContext(Executor executor, Consumer<Throwable> reporter) {
        this.executor = executor;
        this.reporter = reporter;
    }

    @Override
    public void execute(Runnable runnable) {
        executor.execute(runnable);
    }

    @Override
    public void reportFailure(Throwable cause) {
        reporter.accept(cause);
    }
}
