package io.mitallast.io;

import io.mitallast.concurrent.ExecutionContext;
import io.mitallast.higher.Higher;
import io.mitallast.kernel.Unit;

/**
 * ContextShift provides support for shifting execution.
 * <p>
 * The `shift` method inserts an asynchronous boundary, which moves execution
 * from the calling thread to the default execution environment of `F`.
 * <p>
 * The `evalOn` method provides a way to evaluate a task on a specific execution
 * context, shifting back to the default execution context after the task completes.
 * <p>
 * This is NOT a type class, as it does not have the coherence
 * requirement.
 */
public interface ContextShift<F extends Higher> {
    /**
     * Asynchronous boundary described as an effectful `F[_]` that
     * can be used in `flatMap` chains to "shift" the continuation
     * of the run-loop to another thread or call stack.
     * <p>
     * This is the [[Async.shift]] operation, without the need for an
     * `ExecutionContext` taken as a parameter.
     */
    Higher<F, Unit> shift();

    /**
     * Evaluates `f` on the supplied execution context and shifts evaluation
     * back to the default execution environment of `F` at the completion of `f`,
     * regardless of success or failure.
     * <p>
     * The primary use case for this method is executing blocking code on a
     * dedicated execution context.
     *
     * @param ec Execution context where the evaluation has to be scheduled
     * @param fa Computation to evaluate using `ec`
     */
    <A> Higher<F, A> evalOn(ExecutionContext ec, Higher<F, A> fa);
}
