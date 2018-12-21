package io.mitallast.concurrent;

import java.util.function.Supplier;

public interface BlockContext {
    <T> T blockOn(Supplier<T> thunk);

    static BlockContext defaultBlockContext() {
        return DefaultBlockContext.instance();
    }

    static BlockContext current() {
        return DefaultBlockContext.current();
    }

    static <T> T withBlockContext(BlockContext blockContext, Supplier<T> body) {
        return DefaultBlockContext.withBlockContext(blockContext, body);
    }
}

class DefaultBlockContext implements BlockContext {
    private final static BlockContext instance = new DefaultBlockContext();

    private final static ThreadLocal<BlockContext> contextLocal = new ThreadLocal<>();

    public static BlockContext instance() {
        return instance;
    }

    public static BlockContext current() {
        var current = contextLocal.get();
        if (current == null) {
            var ctx = Thread.currentThread();
            if (ctx instanceof BlockContext) {
                return (BlockContext) ctx;
            } else {
                return instance;
            }
        } else {
            return current;
        }
    }

    public static <T> T withBlockContext(BlockContext blockContext, Supplier<T> body) {
        var old = contextLocal.get();
        try {
            contextLocal.set(blockContext);
            return body.get();
        } finally {
            contextLocal.set(old);
        }
    }

    @Override
    public <T> T blockOn(Supplier<T> thunk) {
        return thunk.get();
    }
}