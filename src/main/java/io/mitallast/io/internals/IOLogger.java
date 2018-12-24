package io.mitallast.io.internals;

/**
 * INTERNAL API — logs uncaught exceptions in a platform specific way.
 * <p>
 * For the JVM logging is accomplished using the current
 * [[https://docs.oracle.com/javase/8/docs/api/java/lang/Thread.UncaughtExceptionHandler.html Thread.UncaughtExceptionHandler]].
 * <p>
 * If an `UncaughtExceptionHandler` is not currently set,
 * then error is printed on standard output.
 */
public interface IOLogger {
    static void reportFailure(Throwable e) {
        var handler = Thread.getDefaultUncaughtExceptionHandler();
        if (handler == null) {
            e.printStackTrace();
        } else {
            handler.uncaughtException(Thread.currentThread(), e);
        }
    }
}
