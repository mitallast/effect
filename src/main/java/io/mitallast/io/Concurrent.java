package io.mitallast.io;

import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;
import io.mitallast.product.Tuple2;

import java.util.function.Supplier;

/**
 * Type class for [[Async]] data types that are cancelable and
 * can be started concurrently.
 * <p>
 * Thus this type class allows abstracting over data types that:
 * <p>
 * 1. implement the [[Async]] algebra, with all its restrictions
 * 1. can provide logic for cancellation, to be used in race
 * conditions in order to release resources early
 * (in its [[Concurrent!.cancelable cancelable]] builder)
 * <p>
 * Due to these restrictions, this type class also affords to describe
 * a [[Concurrent!.start start]] operation that can start async
 * processes, suspended in the context of `F[_]` and that can be
 * canceled or joined.
 * <p>
 * Without cancellation being baked in, we couldn't afford to do it.
 * See below.
 * <p>
 * ==Cancelable builder==
 * <p>
 * The signature exposed by the [[Concurrent!.cancelable cancelable]]
 * builder is this:
 * <p>
 * {{{
 * (Either[Throwable, A] => Unit) => CancelToken[F]
 * }}}
 * <p>
 * [[CancelToken CancelToken[F]]] is just an alias for `F[Unit]` and
 * used to represent a cancellation action which will send a signal
 * to the producer, that may observe it and cancel the asynchronous
 * process.
 * <p>
 * ==On Cancellation==
 * <p>
 * Simple asynchronous processes, like Scala's `Future`, can be
 * described with this very basic and side-effectful type and you
 * should recognize what is more or less the signature of
 * `Future#onComplete` or of [[Async.async]] (minus the error
 * handling):
 * <p>
 * {{{
 * (A => Unit) => Unit
 * }}}
 * <p>
 * But many times the abstractions built to deal with asynchronous
 * tasks can also provide a way to cancel such processes, to be used
 * in race conditions in order to cleanup resources early, so a very
 * basic and side-effectful definition of asynchronous processes that
 * can be canceled would be:
 * <p>
 * {{{
 * (A => Unit) => CancelToken
 * }}}
 * <p>
 * This is approximately the signature of JavaScript's `setTimeout`,
 * which will return a "task ID" that can be used to cancel it. Or of
 * Java's `ScheduledExecutorService#schedule`, which will return a
 * Java `ScheduledFuture` that has a `.cancel()` operation on it.
 * <p>
 * Similarly, for `Concurrent` data types, we can provide
 * cancellation logic that can be triggered in race conditions to
 * cancel the on-going processing, only that `Concurrent`'s
 * cancelation token is an action suspended in an `F[Unit]`.
 * <p>
 * Suppose you want to describe a "sleep" operation, like that described
 * by [[Timer]] to mirror Java's `ScheduledExecutorService.schedule`
 * or JavaScript's `setTimeout`:
 * <p>
 * {{{
 * def sleep(d: FiniteDuration): F[Unit]
 * }}}
 * <p>
 * This signature is in fact incomplete for data types that are not
 * cancelable, because such equivalent operations always return some
 * cancellation token that can be used to trigger a forceful
 * interruption of the timer. This is not a normal "dispose" or
 * "finally" clause in a try/catch block, because "cancel" in the
 * context of an asynchronous process is ''concurrent'' with the
 * task's own run-loop.
 * <p>
 * To understand what this means, consider that in the case of our
 * `sleep` as described above, on cancellation we'd need a way to
 * signal to the underlying `ScheduledExecutorService` to forcefully
 * remove the scheduled `Runnable` from its internal queue of
 * scheduled tasks, ''before'' its execution. Therefore, without a
 * cancelable data type, a safe signature needs to return a
 * cancellation token, so it would look like this:
 * <p>
 * {{{
 * def sleep(d: FiniteDuration): F[(F[Unit], F[Unit])]
 * }}}
 * <p>
 * This function is returning a tuple, with one `F[Unit]` to wait for
 * the completion of our sleep and a second `F[Unit]` to cancel the
 * scheduled computation in case we need it. This is in fact the shape
 * of [[Fiber]]'s API. And this is exactly what the
 * [[Concurrent!.start start]] operation returns.
 * <p>
 * The difference between a [[Concurrent]] data type and one that
 * is only [[Async]] is that you can go from any `F[A]` to a
 * `F[Fiber[F, A]]`, to participate in race conditions and that can be
 * canceled should the need arise, in order to trigger an early
 * release of allocated resources.
 * <p>
 * Thus a [[Concurrent]] data type can safely participate in race
 * conditions, whereas a data type that is only [[Async]] cannot do it
 * without exposing and forcing the user to work with cancellation
 * tokens. An [[Async]] data type cannot expose for example a `start`
 * operation that is safe.
 * <p>
 * == Resource-safety ==
 * <p>
 * [[Concurrent]] data types are required to cooperate with [[Bracket]].
 * `Concurrent` being cancelable by law, what this means for the
 * corresponding `Bracket` is that cancelation can be observed and
 * that in the case of [[Bracket.bracketCase bracketCase]] the
 * [[ExitCase.Canceled]] branch will get executed on cancelation.
 * <p>
 * By default the `cancelable` builder is derived from `bracketCase`
 * and from [[Async.asyncF asyncF]], so what this means is that
 * whatever you can express with `cancelable`, you can also express
 * with `bracketCase`.
 * <p>
 * For [[Bracket.uncancelable uncancelable]], the [[Fiber.cancel cancel]]
 * signal has no effect on the result of [[Fiber.join join]] and
 * the cancelable token returned by [[ConcurrentEffect.runCancelable]]
 * on evaluation will have no effect if evaluated.
 * <p>
 * So `uncancelable` must undo the cancellation mechanism of
 * [[Concurrent!.cancelable cancelable]], with this equivalence:
 * <p>
 * {{{
 * F.uncancelable(F.cancelable { cb => f(cb); token }) <-> F.async(f)
 * }}}
 * <p>
 * Sample:
 * <p>
 * {{{
 * val F = Concurrent[IO]
 * val timer = Timer[IO]
 * <p>
 * // Normally Timer#sleep yields cancelable tasks
 * val tick = F.uncancelable(timer.sleep(10.seconds))
 * <p>
 * // This prints "Tick!" after 10 seconds, even if we are
 * // canceling the Fiber after start:
 * for {
 * fiber <- F.start(tick)
 * _ <- fiber.cancel
 * _ <- fiber.join
 * _ <- F.delay { println("Tick!") }
 * } yield ()
 * }}}
 * <p>
 * When doing [[Bracket.bracket bracket]] or [[Bracket.bracketCase bracketCase]],
 * `acquire` and `release` operations are guaranteed to be uncancelable as well.
 */
public interface Concurrent<F extends Higher> extends Async<F> {
    /**
     * Start concurrent execution of the source suspended in
     * the `F` context.
     * <p>
     * Returns a [[Fiber]] that can be used to either join or cancel
     * the running computation, being similar in spirit (but not
     * in implementation) to starting a thread.
     */
    <A> Higher<F, Fiber<F, A>> start(Higher<F, A> fa);

    /**
     * Run two tasks concurrently, creating a race between them and returns a
     * pair containing both the winner's successful value and the loser
     * represented as a still-unfinished fiber.
     * <p>
     * If the first task completes in error, then the result will
     * complete in error, the other task being canceled.
     * <p>
     * On usage the user has the option of canceling the losing task,
     * this being equivalent with plain [[race]]:
     * <p>
     * {{{
     * val ioA: IO[A] = ???
     * val ioB: IO[B] = ???
     * <p>
     * Concurrent[IO].racePair(ioA, ioB).flatMap {
     * case Left((a, fiberB)) =>
     * fiberB.cancel.map(_ => a)
     * case Right((fiberA, b)) =>
     * fiberA.cancel.map(_ => b)
     * }
     * }}}
     * <p>
     * See [[race]] for a simpler version that cancels the loser
     * immediately.
     */
    <A, B>
    Higher<F, Either<Tuple2<A, Fiber<F, B>>, Tuple2<Fiber<F, A>, B>>>
    racePair(Higher<F, A> fa, Higher<F, B> fb);

    /**
     * Run two tasks concurrently and return the first to finish,
     * either in success or error. The loser of the race is canceled.
     * <p>
     * The two tasks are potentially executed in parallel, the winner
     * being the first that signals a result.
     * <p>
     * As an example see [[Concurrent.timeoutTo]]
     * <p>
     * Also see [[racePair]] for a version that does not cancel
     * the loser automatically on successful results.
     */
    default <A, B> Higher<F, Either<A, B>> race(Higher<F, A> fa, Higher<F, B> fb) {
        return flatMap(racePair(fa, fb), either -> either.fold(
            tuple -> {
                var a = tuple.t1();
                var fiberB = tuple.t2();
                return map(fiberB.cancel(), u -> Either.left(a));
            },
            tuple -> {
                var fiberA = tuple.t1();
                var b = tuple.t2();
                return map(fiberA.cancel(), u -> Either.right(b));
            }
        ));
    }

    /**
     * Creates a cancelable `F[A]` instance that executes an
     * asynchronous process on evaluation.
     * <p>
     * This builder accepts a registration function that is
     * being injected with a side-effectful callback, to be called
     * when the asynchronous process is complete with a final result.
     * <p>
     * The registration function is also supposed to return
     * a [[CancelToken]], which is nothing more than an
     * alias for `F[Unit]`, capturing the logic necessary for
     * canceling the asynchronous process for as long as it
     * is still active.
     * <p>
     * Example:
     * <p>
     * {{{
     * import java.util.concurrent.ScheduledExecutorService
     * import scala.concurrent.duration._
     * <p>
     * def sleep[F[_]](d: FiniteDuration)
     * (implicit F: Concurrent[F], ec: ScheduledExecutorService): F[Unit] = {
     * <p>
     * F.cancelable { cb =>
     * // Schedules task to run after delay
     * val run = new Runnable { def run() = cb(Right(())) }
     * val future = ec.schedule(run, d.length, d.unit)
     * <p>
     * // Cancellation logic, suspended in F
     * F.delay(future.cancel(true))
     * }
     * }
     * }}}
     */
    default <A> Higher<F, A> cancelable(Function1<Supplier<Either<Throwable, A>>, Higher<F, Unit>> k) {
        // @todo implement
        return null;
    }

    /**
     * Inherited from [[LiftIO]], defines a conversion from [[IO]]
     * in terms of the `Concurrent` type class.
     * <p>
     * N.B. expressing this conversion in terms of `Concurrent` and
     * its capabilities means that the resulting `F` is cancelable in
     * case the source `IO` is.
     * <p>
     * To access this implementation as a standalone function, you can
     * use [[Concurrent$.liftIO Concurrent.liftIO]]
     * (on the object companion).
     */
    default <A> Higher<F, A> liftIO(IO<A> ioa) {
        // @todo implement
        return null;
    }
}
