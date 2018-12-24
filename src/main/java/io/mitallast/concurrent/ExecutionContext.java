package io.mitallast.concurrent;

/**
 * An `ExecutionContext` can execute program logic asynchronously,
 * typically but not necessarily on a thread pool.
 * <p>
 * A general purpose `ExecutionContext` must be asynchronous in executing
 * any `Runnable` that is passed into its `execute`-method. A special purpose
 * `ExecutionContext` may be synchronous but must only be passed to code that
 * is explicitly safe to be run using a synchronously executing `ExecutionContext`.
 * <p>
 * APIs such as `Future.onComplete` require you to provide a callback
 * and an implicit `ExecutionContext`. The implicit `ExecutionContext`
 * will be used to execute the callback.
 * <p>
 * While it is possible to simply import
 * `scala.concurrent.ExecutionContext.Implicits.global` to obtain an
 * implicit `ExecutionContext`, application developers should carefully
 * consider where they want to set execution policy;
 * ideally, one place per application—or per logically related section of code—
 * will make a decision about which `ExecutionContext` to use.
 * That is, you will mostly want to avoid hardcoding, especially via an import,
 * `scala.concurrent.ExecutionContext.Implicits.global`.
 * The recommended approach is to add `(implicit ec: ExecutionContext)` to methods,
 * or class constructor parameters, which need an `ExecutionContext`.
 * <p>
 * Then locally import a specific `ExecutionContext` in one place for the entire
 * application or module, passing it implicitly to individual methods.
 * Alternatively define a local implicit val with the required `ExecutionContext`.
 * <p>
 * A custom `ExecutionContext` may be appropriate to execute code
 * which blocks on IO or performs long-running computations.
 * `ExecutionContext.fromExecutorService` and `ExecutionContext.fromExecutor`
 * are good ways to create a custom `ExecutionContext`.
 * <p>
 * The intent of `ExecutionContext` is to lexically scope code execution.
 * That is, each method, class, file, package, or application determines
 * how to run its own code. This avoids issues such as running
 * application callbacks on a thread pool belonging to a networking library.
 * The size of a networking library's thread pool can be safely configured,
 * knowing that only that library's network operations will be affected.
 * Application callback execution can be configured separately.
 */
public interface ExecutionContext {
    /**
     * Runs a block of code on this execution context.
     *
     * @param runnable the task to execute
     */
    void execute(Runnable runnable);

    /**
     * Reports that an asynchronous computation failed.
     *
     * @param cause the cause of the failure
     */
    void reportFailure(Throwable cause);

    static <T> T blocking(Task<T> body) {
        return BlockContext.current().blockOn(body);
    }
}
