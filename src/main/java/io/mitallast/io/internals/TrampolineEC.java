package io.mitallast.io.internals;

import io.mitallast.concurrent.BlockContext;
import io.mitallast.concurrent.ExecutionContext;
import io.mitallast.kernel.Unit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

final class TrampolineEC implements ExecutionContext {
    private final static Logger logger = LogManager.getLogger();

    private final ExecutionContext underlying;

    private final ThreadLocal<Trampoline> trampoline = new ThreadLocal<>() {
        @Override
        protected Trampoline initialValue() {
            return new BlockingTrampoline(underlying);
        }
    };

    private TrampolineEC(ExecutionContext underlying) {
        this.underlying = underlying;
    }

    @Override
    public void execute(Runnable runnable) {
        trampoline.get().execute(runnable);
    }

    @Override
    public void reportFailure(Throwable e) {
        underlying.reportFailure(e);
    }

    public static TrampolineEC immediate = new TrampolineEC(new ExecutionContext() {
        @Override
        public void execute(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void reportFailure(Throwable e) {
            logger.fatal(e);
        }
    });

    private static class BlockingTrampoline extends Trampoline {
        private final BlockContext trampolineContext = new BlockContext() {
            @Override
            public <T> T blockOn(Supplier<T> thunk) {
                forkTheRest();
                return thunk.get();
            }
        };

        BlockingTrampoline(ExecutionContext underlying) {
            super(underlying);
        }

        @Override
        public void startLoop(Runnable runnable) {
            BlockContext.withBlockContext(trampolineContext, () -> {
                super.startLoop(runnable);
                return Unit.unit();
            });
        }
    }
}
