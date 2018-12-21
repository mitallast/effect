package io.mitallast.concurrent;

public interface ExecutionContext {
    void execute(Runnable runnable);
    void reportFailure(Throwable e);
}
