package io.mitallast.concurrent;

public interface Task<T> {
    T run() throws Exception;

    default T runUnsafe() {
        try {
            return run();
        } catch (Exception e) {
            return throwsUnchecked(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <R, T extends Exception> R throwsUnchecked(Exception toThrow) throws T {
        // Since the type is erased, this cast actually does nothing!!!
        // we can throw any exception
        throw (T) toThrow;
    }
}
