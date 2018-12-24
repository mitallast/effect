package io.mitallast.io;

import io.mitallast.categories.MonadError;
import io.mitallast.higher.Higher;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Function2;

/**
 * An extension of `MonadError` exposing the `bracket` operation,
 * a generalized abstracted pattern of safe resource acquisition and
 * release in the face of errors or interruption.
 *
 * @define acquireParam is an action that "acquires" some expensive
 * resource, that needs to be used and then discarded
 * @define useParam is the action that uses the newly allocated
 * resource and that will provide the final result
 */
public interface Bracket<F extends Higher, E> extends MonadError<F, E> {
    /**
     * A generalized version of [[bracket]] which uses [[ExitCase]]
     * to distinguish between different exit cases when releasing
     * the acquired resource.
     *
     * @param acquire $acquireParam
     * @param use     $useParam
     * @param release is the action that's supposed to release the
     *                allocated resource after `use` is done, by observing
     *                and acting on its exit condition
     */
    <A, B> Higher<F, B> bracketCase(final Higher<F, A> acquire,
                                    final Function1<A, Higher<F, B>> use,
                                    final Function2<A, ExitCase<E>, Higher<F, Unit>> release);

    /**
     * Operation meant for specifying tasks with safe resource
     * acquisition and release in the face of errors and interruption.
     * <p>
     * This operation provides the equivalent of `try/catch/finally`
     * statements in mainstream imperative languages for resource
     * acquisition and release.
     *
     * @param acquire $acquireParam
     * @param use     $useParam
     * @param release is the action that's supposed to release the
     *                allocated resource after `use` is done, irregardless of
     *                its exit condition
     */
    default <A, B> Higher<F, B> bracket(final Higher<F, A> acquire,
                                        final Function1<A, Higher<F, B>> use,
                                        final Function1<A, Higher<F, Unit>> release) {
        return bracketCase(
            acquire,
            use,
            (a, ignore) -> release.apply(a)
        );
    }

    /**
     * Operation meant for ensuring a given task continues execution even
     * when interrupted.
     */
    default <A> Higher<F, A> uncancelable(Higher<F, A> fa) {
        return bracket(fa, a -> this.pure(a), ignore -> unit());
    }

    /**
     * Executes the given `finalizer` when the source is finished,
     * either in success or in error, or if canceled.
     * <p>
     * This variant of [[guaranteeCase]] evaluates the given `finalizer`
     * regardless of how the source gets terminated:
     * <p>
     * - normal completion
     * - completion in error
     * - cancelation
     * <p>
     * This equivalence always holds:
     * <p>
     * {{{
     * F.guarantee(fa)(f) <-> F.bracket(F.unit)(_ => fa)(_ => f)
     * }}}
     * <p>
     * As best practice, it's not a good idea to release resources
     * via `guaranteeCase` in polymorphic code. Prefer [[bracket]]
     * for the acquisition and release of resources.
     *
     * @see [[guaranteeCase]] for the version that can discriminate
     * between termination conditions
     * @see [[bracket]] for the more general operation
     */
    default <A> Higher<F, A> guarantee(Higher<F, A> fa, Higher<F, Unit> finalizer) {
        return bracket(unit(), ignore -> fa, ignore -> finalizer);
    }

    /**
     * Executes the given `finalizer` when the source is finished,
     * either in success or in error, or if canceled, allowing
     * for differentiating between exit conditions.
     * <p>
     * This variant of [[guarantee]] injects an [[ExitCase]] in
     * the provided function, allowing one to make a difference
     * between:
     * <p>
     * - normal completion
     * - completion in error
     * - cancelation
     * <p>
     * This equivalence always holds:
     * <p>
     * {{{
     * F.guaranteeCase(fa)(f) <-> F.bracketCase(F.unit)(_ => fa)((_, e) => f(e))
     * }}}
     * <p>
     * As best practice, it's not a good idea to release resources
     * via `guaranteeCase` in polymorphic code. Prefer [[bracketCase]]
     * for the acquisition and release of resources.
     *
     * @see [[guarantee]] for the simpler version
     * @see [[bracketCase]] for the more general operation
     */

    default <A> Higher<F, A> guaranteeCase(Higher<F, A> fa, Function1<ExitCase<E>, Higher<F, Unit>> finalizer) {
        return bracketCase(unit(), ignore -> fa, (ignore, e) -> finalizer.apply(e));
    }
}


