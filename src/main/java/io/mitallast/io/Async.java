package io.mitallast.io;

import io.mitallast.concurrent.ExecutionContext;
import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.io.internals.IORunLoop;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * A monad that can describe asynchronous or synchronous computations
 * that produce exactly one result.
 * <p>
 * ==On Asynchrony==
 * <p>
 * An asynchronous task represents logic that executes independent of
 * the main program flow, or current callstack. It can be a task whose
 * result gets computed on another thread, or on some other machine on
 * the network.
 * <p>
 * In terms of types, normally asynchronous processes are represented as:
 * {{{
 * (A => Unit) => Unit
 * }}}
 * <p>
 * This signature can be recognized in the "Observer pattern" described
 * in the "Gang of Four", although it should be noted that without
 * an `onComplete` event (like in the Rx Observable pattern) you can't
 * detect completion in case this callback can be called zero or
 * multiple times.
 * <p>
 * Some abstractions allow for signaling an error condition
 * (e.g. `MonadError` data types), so this would be a signature
 * that's closer to Scala's `Future#onComplete`:
 * <p>
 * {{{
 * (Either[Throwable, A] => Unit) => Unit
 * }}}
 * <p>
 * And many times the abstractions built to deal with asynchronous tasks
 * also provide a way to cancel such processes, to be used in race
 * conditions in order to cleanup resources early:
 * <p>
 * {{{
 * (A => Unit) => Cancelable
 * }}}
 * <p>
 * This is approximately the signature of JavaScript's `setTimeout`,
 * which will return a "task ID" that can be used to cancel it.
 * <p>
 * N.B. this type class in particular is NOT describing cancelable
 * async processes, see the [[Concurrent]] type class for that.
 * <p>
 * ==Async Type class==
 * <p>
 * This type class allows the modeling of data types that:
 * <p>
 * 1. can start asynchronous processes
 * 1. can emit one result on completion
 * 1. can end in error
 * <p>
 * N.B. on the "one result" signaling, this is not an ''exactly once''
 * requirement. At this point streaming types can implement `Async`
 * and such an ''exactly once'' requirement is only clear in [[Effect]].
 * <p>
 * Therefore the signature exposed by the [[Async!.async async]]
 * builder is this:
 * <p>
 * {{{
 * (Either[Throwable, A] => Unit) => Unit
 * }}}
 * <p>
 * N.B. such asynchronous processes are not cancelable.
 * See the [[Concurrent]] alternative for that.
 */
public interface Async<F extends Higher> extends LiftIO<F>, Sync<F> {
    /**
     * Creates a simple, non-cancelable `F[A]` instance that
     * executes an asynchronous process on evaluation.
     * <p>
     * The given function is being injected with a side-effectful
     * callback for signaling the final result of an asynchronous
     * process.
     * <p>
     * This operation could be derived from [[asyncF]], because:
     * <p>
     * {{{
     * F.async(k) <-> F.asyncF(cb => F.delay(k(cb)))
     * }}}
     * <p>
     * As an example of wrapping an impure async API, here's the
     * implementation of [[Async.shift]]:
     * <p>
     * {{{
     * def shift[F[_]](ec: ExecutionContext)(implicit F: Async[F]): F[Unit] =
     * F.async { cb =>
     * // Scheduling an async boundary (logical thread fork)
     * ec.execute(new Runnable {
     * def run(): Unit = {
     * // Signaling successful completion
     * cb(Right(()))
     * }
     * })
     * }
     * }}}
     *
     * @param k is a function that should be called with a
     *          callback for signaling the result once it is ready
     * @see [[asyncF]] for the variant that can suspend side effects
     * in the provided registration function.
     */
    <A> Higher<F, A> async(Consumer<Consumer<Either<Throwable, A>>> k);

    /**
     * Creates a simple, non-cancelable `F[A]` instance that
     * executes an asynchronous process on evaluation.
     * <p>
     * The given function is being injected with a side-effectful
     * callback for signaling the final result of an asynchronous
     * process. And its returned result needs to be a pure `F[Unit]`
     * that gets evaluated by the runtime.
     * <p>
     * Note the simpler async variant [[async]] can be derived like this:
     * <p>
     * {{{
     * F.async(k) <-> F.asyncF(cb => F.delay(k(cb)))
     * }}}
     * <p>
     * For wrapping impure APIs usually you can use the simpler [[async]],
     * however `asyncF` is useful in cases where impure APIs are
     * wrapped with the help of pure abstractions, such as
     * [[cats.effect.concurrent.Ref Ref]].
     * <p>
     * For example here's how a simple, "pure Promise" implementation
     * could be implemented via `Ref` (sample is for didactic purposes,
     * as you have a far better
     * [[cats.effect.concurrent.Deferred Deferred]] available):
     * <p>
     * {{{
     * import cats.effect.concurrent.Ref
     * <p>
     * type Callback[-A] = Either[Throwable, A] => Unit
     * <p>
     * class PurePromise[F[_], A](ref: Ref[F, Either[List[Callback[A]], A]])
     * (implicit F: Async[F]) {
     * <p>
     * def get: F[A] = F.asyncF { cb =>
     * ref.modify {
     * case current @ Right(result) =>
     * (current, F.delay(cb(Right(result))))
     * case Left(list) =>
     * (Left(cb :: list), F.unit)
     * }
     * }
     * <p>
     * def complete(value: A): F[Unit] =
     * F.flatten(ref.modify {
     * case Left(list) =>
     * (Right(value), F.delay(list.foreach(_(Right(value)))))
     * case right =>
     * (right, F.unit)
     * })
     * }
     * }}}
     * <p>
     * N.B. if `F[_]` is a cancelable data type (i.e. implementing
     * [[Concurrent]]), then the returned `F[Unit]` can be cancelable,
     * its evaluation hooking into the underlying cancelation mechanism
     * of `F[_]`, so something like this behaves like you'd expect:
     * <p>
     * {{{
     * def delayed[F[_], A](thunk: => A)
     * (implicit F: Async[F], timer: Timer[F]): F[A] = {
     * <p>
     * timer.sleep(1.second) *> F.delay(cb(
     * try cb(Right(thunk))
     * catch { case NonFatal(e) => Left(cb(Left(e))) }
     * ))
     * }
     * }}}
     * <p>
     * The `asyncF` operation behaves like [[Sync.suspend]], except
     * that the result has to be signaled via the provided callback.
     * <p>
     * ==ERROR HANDLING==
     * <p>
     * As a matter of contract the returned `F[Unit]` should not
     * throw errors. If it does, then the behavior is undefined.
     * <p>
     * This is because by contract the provided callback should
     * only be called once. Calling it concurrently, multiple times,
     * is a contract violation. And if the returned `F[Unit]` throws,
     * then the implementation might have called it already, so it
     * would be a contract violation to call it without expensive
     * synchronization.
     * <p>
     * In case errors are thrown the behavior is implementation specific.
     * The error might get logged to stderr, or via other mechanisms
     * that are implementations specific.
     *
     * @param k is a function that should be called with a
     *          callback for signaling the result once it is ready
     * @see [[async]] for the simpler variant.
     */
    <A> Higher<F, A> asyncF(Function1<Consumer<Either<Throwable, A>>, Higher<F, Unit>> k);

    /**
     * Inherited from [[LiftIO]], defines a conversion from [[IO]]
     * in terms of the `Async` type class.
     * <p>
     * N.B. expressing this conversion in terms of `Async` and its
     * capabilities means that the resulting `F` is not cancelable.
     * [[Concurrent]] then overrides this with an implementation
     * that is.
     * <p>
     * To access this implementation as a standalone function, you can
     * use [[Async$.liftIO Async.liftIO]] (on the object companion).
     */
    @Override
    default <A> Higher<F, A> liftIO(IO<A> ioa) {
        return liftIO(ioa, this);
    }

    default <A> Higher<F, A> never() {
        return async(c -> {
        });
    }

    default Higher<F, Unit> shift(Executor ec) {
        return async(cb -> ec.execute(() -> cb.accept(Either.right(Unit.unit()))));
    }

    static <F extends Higher> Higher<F, Unit> shift(ExecutionContext ec, Async<F> F) {
        return F.async(cb -> ec.execute(() -> cb.accept(Either.right(Unit.unit()))));
    }

    /**
     * Lifts any `IO` value into any data type implementing [[Async]].
     * <p>
     * This is the default `Async.liftIO` implementation.
     */
    static <F extends Higher, A> Higher<F, A> liftIO(IO<A> io, Async<F> F) {
        if (io instanceof IO.Pure) {
            return F.pure(((IO.Pure<A>) io).a());
        } else if (io instanceof IO.RaiseError) {
            return F.raiseError(((IO.RaiseError<A>) io).e());
        } else if (io instanceof IO.Delay) {
            return F.delay(((IO.Delay<A>) io).thunk());
        } else {
            return F.suspend(() -> {
                var s = IORunLoop.step(io);
                if (s instanceof IO.Pure) {
                    return F.pure(((IO.Pure<A>) s).a());
                } else if (s instanceof IO.RaiseError) {
                    return F.raiseError(((IO.RaiseError<A>) s).e());
                } else {
                    return F.async(s::unsafeRunAsync);
                }
            });
        }
    }
}