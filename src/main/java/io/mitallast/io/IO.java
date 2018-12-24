package io.mitallast.io;

import io.mitallast.categories.StackSafeMonad;
import io.mitallast.concurrent.ExecutionContext;
import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.io.internals.*;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Function2;
import io.mitallast.lambda.Function4;
import io.mitallast.maybe.Maybe;
import io.mitallast.product.Tuple2;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.mitallast.io.internals.IOPlatform.fusionMaxStackDepth;

public abstract class IO<A> implements Higher<IO, A> {
    private IO() {
    }

    public <B> IO<B> map(Function1<A, B> f) {
        return new Map<>(this, f, 0);
    }

    public <B> IO<B> flatMap(Function1<A, IO<B>> f) {
        return new Bind<>(this, f);
    }

    public IO<Either<Throwable, A>> attempt() {
        return new Bind<>(this, new AttemptIO<>());
    }

    /**
     * Produces an `IO` reference that should execute the source on
     * evaluation, without waiting for its result, being the safe
     * analogue to [[unsafeRunAsync]].
     * <p>
     * This operation is isomorphic to [[unsafeRunAsync]]. What it does
     * is to let you describe asynchronous execution with a function
     * that stores off the results of the original `IO` as a
     * side effect, thus ''avoiding'' the usage of impure callbacks or
     * eager evaluation.
     * <p>
     * The returned `IO` is guaranteed to execute immediately,
     * and does not wait on any async action to complete, thus this
     * is safe to do, even on top of runtimes that cannot block threads
     * (e.g. JavaScript):
     * <p>
     * {{{
     * // Sample
     * val source = IO.shift *> IO(1)
     * // Describes execution
     * val start = source.runAsync {
     * case Left(e) => IO(e.printStackTrace())
     * case Right(_) => IO.unit
     * }
     * // Safe, because it does not block for the source to finish
     * start.unsafeRunSync
     * }}}
     *
     * @return an `IO` value that upon evaluation will execute the source,
     * but will not wait for its completion
     * @see [[runCancelable]] for the version that gives you a cancelable
     * token that can be used to send a cancel signal
     */
    public final SyncIO<Unit> runAsync(Function1<Either<Throwable, A>, IO<Unit>> cb) {
        return SyncIO.apply(() -> {
            unsafeRunAsync(e -> cb.apply(e).unsafeRunAsyncAndForget());
            return Unit.unit();
        });
    }

    /**
     * Produces an `IO` reference that should execute the source on evaluation,
     * without waiting for its result and return a cancelable token, being the
     * safe analogue to [[unsafeRunCancelable]].
     * <p>
     * This operation is isomorphic to [[unsafeRunCancelable]]. Just like
     * [[runAsync]], this operation avoids the usage of impure callbacks or
     * eager evaluation.
     * <p>
     * The returned `IO` boxes an `IO[Unit]` that can be used to cancel the
     * running asynchronous computation (if the source can be canceled).
     * <p>
     * The returned `IO` is guaranteed to execute immediately,
     * and does not wait on any async action to complete, thus this
     * is safe to do, even on top of runtimes that cannot block threads
     * (e.g. JavaScript):
     * <p>
     * {{{
     * val source: IO[Int] = ???
     * // Describes interruptible execution
     * val start: IO[CancelToken[IO]] = source.runCancelable
     * <p>
     * // Safe, because it does not block for the source to finish
     * val cancel: IO[Unit] = start.unsafeRunSync
     * <p>
     * // Safe, because cancellation only sends a signal,
     * // but doesn't back-pressure on anything
     * cancel.unsafeRunSync
     * }}}
     *
     * @return an `IO` value that upon evaluation will execute the source,
     * but will not wait for its completion, yielding a cancellation
     * token that can be used to cancel the async process
     * @see [[runAsync]] for the simple, uninterruptible version
     */
    public final SyncIO<IO<Unit>> runCancelable(Function1<Either<Throwable, A>, IO<Unit>> cb) {
        return SyncIO.apply(() -> unsafeRunCancelable(e -> cb.apply(e).unsafeRunAsyncAndForget()));
    }

    /**
     * Produces the result by running the encapsulated effects as impure
     * side effects.
     * <p>
     * If any component of the computation is asynchronous, the current
     * thread will block awaiting the results of the async computation.
     * On JavaScript, an exception will be thrown instead to avoid
     * generating a deadlock. By default, this blocking will be
     * unbounded.  To limit the thread block to some fixed time, use
     * `unsafeRunTimed` instead.
     * <p>
     * Any exceptions raised within the effect will be re-thrown during
     * evaluation.
     * <p>
     * As the name says, this is an UNSAFE function as it is impure and
     * performs side effects, not to mention blocking, throwing
     * exceptions, and doing other things that are at odds with
     * reasonable software.  You should ideally only call this function
     * *once*, at the very end of your program.
     */
    public final A unsafeRunSync() {
        return unsafeRunTimed(Duration.ofMillis(Long.MAX_VALUE)).get();
    }

    /**
     * Passes the result of the encapsulated effects to the given
     * callback by running them as impure side effects.
     * <p>
     * Any exceptions raised within the effect will be passed to the
     * callback in the `Either`.  The callback will be invoked at most
     * *once*.  Note that it is very possible to construct an IO which
     * never returns while still never blocking a thread, and attempting
     * to evaluate that IO with this method will result in a situation
     * where the callback is *never* invoked.
     * <p>
     * As the name says, this is an UNSAFE function as it is impure and
     * performs side effects.  You should ideally only call this
     * function ''once'', at the very end of your program.
     */
    public final void unsafeRunAsync(Consumer<Either<Throwable, A>> cb) {
        IORunLoop.start(this, cb);
    }

    /**
     * Triggers the evaluation of the source and any suspended side
     * effects therein, but ignores the result.
     * <p>
     * This operation is similar to [[unsafeRunAsync]], in that the
     * evaluation can happen asynchronously (depending on the source),
     * however no callback is required, therefore the result gets
     * ignored.
     * <p>
     * Note that errors still get logged (via IO's internal logger),
     * because errors being thrown should never be totally silent.
     */
    public final void unsafeRunAsyncAndForget() {
        unsafeRunAsync(Callback.report());
    }

    /**
     * Evaluates the source `IO`, passing the result of the encapsulated
     * effects to the given callback.
     * <p>
     * As the name says, this is an UNSAFE function as it is impure and
     * performs side effects.  You should ideally only call this
     * function ''once'', at the very end of your program.
     *
     * @return an side-effectful function that, when executed, sends a
     * cancellation reference to `IO`'s run-loop implementation,
     * having the potential to interrupt it.
     */
    public final IO<Unit> unsafeRunCancelable(Consumer<Either<Throwable, A>> cb) {
        var conn = IOConnection.apply();
        IORunLoop.startCancelable(this, conn, cb);
        return conn.cancel();
    }

    /**
     * Similar to `unsafeRunSync`, except with a bounded blocking
     * duration when awaiting asynchronous results.
     * <p>
     * Please note that the `limit` parameter does not limit the time of
     * the total computation, but rather acts as an upper bound on any
     * *individual* asynchronous block.  Thus, if you pass a limit of `5
     * seconds` to an `IO` consisting solely of synchronous actions, the
     * evaluation may take considerably longer than 5 seconds!
     * Furthermore, if you pass a limit of `5 seconds` to an `IO`
     * consisting of several asynchronous actions joined together,
     * evaluation may take up to `n * 5 seconds`, where `n` is the
     * number of joined async actions.
     * <p>
     * As soon as an async blocking limit is hit, evaluation
     * ''immediately'' aborts and `None` is returned.
     * <p>
     * Please note that this function is intended for ''testing''; it
     * should never appear in your mainline production code!  It is
     * absolutely not an appropriate function to use if you want to
     * implement timeouts, or anything similar. If you need that sort
     * of functionality, you should be using a streaming library (like
     * fs2 or Monix).
     *
     * @see [[unsafeRunSync]]
     * @see [[timeout]] for pure and safe version
     */
    public final Maybe<A> unsafeRunTimed(Duration limit) {
        var s = IORunLoop.step(this);
        if (s instanceof Pure) {
            return Maybe.apply(((Pure<A>) s).a());
        } else if (s instanceof RaiseError) {
            return IOPlatform.throwsUnchecked(((RaiseError<A>) s).e());
        } else if (s instanceof Async) {
            return IOPlatform.unsafeResync(this, limit);
        } else {
            throw new AssertionError("unreachable");
        }
    }

    /**
     * Evaluates the effect and produces the result in a `Future`.
     * <p>
     * This is similar to `unsafeRunAsync` in that it evaluates the `IO`
     * as a side effect in a non-blocking fashion, but uses a `Future`
     * rather than an explicit callback.  This function should really
     * only be used if interoperating with legacy code which uses Scala
     * futures.
     *
     * @see [[IO.fromFuture]]
     */
    public final CompletableFuture<A> unsafeToFuture() {
        var p = new CompletableFuture<A>();
        unsafeRunAsync(e -> e.foreach(p::completeExceptionally, p::complete));
        return p;
    }

    /**
     * Start execution of the source suspended in the `IO` context.
     * <p>
     * This can be used for non-deterministic / concurrent execution.
     * The following code is more or less equivalent with `parMap2`
     * (minus the behavior on error handling and cancellation):
     * <p>
     * {{{
     * def par2[A, B](ioa: IO[A], iob: IO[B]): IO[(A, B)] =
     * for {
     * fa <- ioa.start
     * fb <- iob.start
     * a <- fa.join
     * b <- fb.join
     * } yield (a, b)
     * }}}
     * <p>
     * Note in such a case usage of `parMapN` (via `cats.Parallel`) is
     * still recommended because of behavior on error and cancellation —
     * consider in the example above what would happen if the first task
     * finishes in error. In that case the second task doesn't get canceled,
     * which creates a potential memory leak.
     */
    public final IO<Fiber<IO, A>> start(ContextShift<IO> cs) {
        return IOStart.apply(cs, this);
    }

    public final IO<A> uncancelable() {
        return IOCancel.uncancelable(this);
    }

    public final <F extends Higher> Higher<F, A> to(LiftIO<F> F) {
        return F.liftIO(this);
    }

    /**
     * Returns an IO that either completes with the result of the source within
     * the specified time `duration` or otherwise evaluates the `fallback`.
     * <p>
     * The source is cancelled in the event that it takes longer than
     * the `FiniteDuration` to complete, the evaluation of the fallback
     * happening immediately after that.
     *
     * @param duration is the time span for which we wait for the source to
     *                 complete; in the event that the specified time has passed without
     *                 the source completing, the `fallback` gets evaluated
     * @param fallback is the task evaluated after the duration has passed and
     *                 the source canceled
     * @param timer    is an implicit requirement for the ability to do a fiber
     *                 [[Timer.sleep sleep]] for the specified timeout, at which point
     *                 the fallback needs to be triggered
     * @param cs       is an implicit requirement for the ability to trigger a
     *                 [[IO.race race]], needed because IO's `race` operation automatically
     *                 forks the involved tasks
     */
    public final IO<A> timeoutTo(Duration duration,
                                 IO<A> fallback,
                                 Timer<IO> timer,
                                 ContextShift<IO> cs) {
        var ce = new IOConcurrentEffect(cs);
        return (IO<A>) Concurrent.timeoutTo(this, duration, fallback, ce, timer);
    }

    public final IO<A> timeout(Duration duration,
                               Timer<IO> timer,
                               ContextShift<IO> cs) {
        return timeoutTo(duration, IO.raiseError(new TimeoutException(duration.toString())), timer, cs);
    }

    /**
     * Returns an `IO` action that treats the source task as the
     * acquisition of a resource, which is then exploited by the `use`
     * function and then `released`.
     * <p>
     * The `bracket` operation is the equivalent of the
     * `try {} catch {} finally {}` statements from mainstream languages.
     * <p>
     * The `bracket` operation installs the necessary exception handler
     * to release the resource in the event of an exception being raised
     * during the computation, or in case of cancellation.
     * <p>
     * If an exception is raised, then `bracket` will re-raise the
     * exception ''after'' performing the `release`. If the resulting
     * task gets canceled, then `bracket` will still perform the
     * `release`, but the yielded task will be non-terminating
     * (equivalent with [[IO.never]]).
     * <p>
     * Example:
     * <p>
     * {{{
     * import java.io._
     * <p>
     * def readFile(file: File): IO[String] = {
     * // Opening a file handle for reading text
     * val acquire = IO(new BufferedReader(
     * new InputStreamReader(new FileInputStream(file), "utf-8")
     * ))
     * <p>
     * acquire.bracket { in =>
     * // Usage part
     * IO {
     * // Yes, ugly Java, non-FP loop;
     * // side-effects are suspended though
     * var line: String = null
     * val buff = new StringBuilder()
     * do {
     * line = in.readLine()
     * if (line != null) buff.append(line)
     * } while (line != null)
     * buff.toString()
     * }
     * } { in =>
     * // The release part
     * IO(in.close())
     * }
     * }
     * }}}
     * <p>
     * Note that in case of cancellation the underlying implementation
     * cannot guarantee that the computation described by `use` doesn't
     * end up executed concurrently with the computation from
     * `release`. In the example above that ugly Java loop might end up
     * reading from a `BufferedReader` that is already closed due to the
     * task being canceled, thus triggering an error in the background
     * with nowhere to get signaled.
     * <p>
     * In this particular example, given that we are just reading from a
     * file, it doesn't matter. But in other cases it might matter, as
     * concurrency on top of the JVM when dealing with I/O might lead to
     * corrupted data.
     * <p>
     * For those cases you might want to do synchronization (e.g. usage
     * of locks and semaphores) and you might want to use [[bracketCase]],
     * the version that allows you to differentiate between normal
     * termination and cancellation.
     * <p>
     * '''NOTE on error handling''': one big difference versus
     * `try/finally` statements is that, in case both the `release`
     * function and the `use` function throws, the error raised by `use`
     * gets signaled.
     * <p>
     * For example:
     * <p>
     * {{{
     * IO("resource").bracket { _ =>
     * // use
     * IO.raiseError(new RuntimeException("Foo"))
     * } { _ =>
     * // release
     * IO.raiseError(new RuntimeException("Bar"))
     * }
     * }}}
     * <p>
     * In this case the error signaled downstream is `"Foo"`, while the
     * `"Bar"` error gets reported. This is consistent with the behavior
     * of Haskell's `bracket` operation and NOT with `try {} finally {}`
     * from Scala, Java or JavaScript.
     *
     * @param use     is a function that evaluates the resource yielded by
     *                the source, yielding a result that will get generated by
     *                the task returned by this `bracket` function
     * @param release is a function that gets called after `use`
     *                terminates, either normally or in error, or if it gets
     *                canceled, receiving as input the resource that needs to
     *                be released
     * @see [[bracketCase]]
     */
    public final <B> IO<B> bracket(Function1<A, IO<B>> use, Function1<A, IO<Unit>> release) {
        return bracketCase(use, (a, ec) -> release.apply(a));
    }

    /**
     * Returns a new `IO` task that treats the source task as the
     * acquisition of a resource, which is then exploited by the `use`
     * function and then `released`, with the possibility of
     * distinguishing between normal termination and cancellation, such
     * that an appropriate release of resources can be executed.
     * <p>
     * The `bracketCase` operation is the equivalent of
     * `try {} catch {} finally {}` statements from mainstream languages
     * when used for the acquisition and release of resources.
     * <p>
     * The `bracketCase` operation installs the necessary exception handler
     * to release the resource in the event of an exception being raised
     * during the computation, or in case of cancellation.
     * <p>
     * In comparison with the simpler [[bracket]] version, this one
     * allows the caller to differentiate between normal termination,
     * termination in error and cancellation via an [[ExitCase]]
     * parameter.
     *
     * @param use     is a function that evaluates the resource yielded by
     *                the source, yielding a result that will get generated by
     *                this function on evaluation
     * @param release is a function that gets called after `use`
     *                terminates, either normally or in error, or if it gets
     *                canceled, receiving as input the resource that needs that
     *                needs release, along with the result of `use`
     *                (cancellation, error or successful result)
     * @see [[bracket]]
     */
    public final <B> IO<B> bracketCase(Function1<A, IO<B>> use, Function2<A, ExitCase<Throwable>, IO<Unit>> release) {
        return IOBracket.apply(this, use, release);
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
     * io.guarantee(f) <-> IO.unit.bracket(_ => io)(_ => f)
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
    public final IO<A> guarantee(IO<Unit> finalizer) {
        return guaranteeCase(ec -> finalizer);
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
     * io.guaranteeCase(f) <-> IO.unit.bracketCase(_ => io)((_, e) => f(e))
     * }}}
     * <p>
     * As best practice, it's not a good idea to release resources
     * via `guaranteeCase` in polymorphic code. Prefer [[bracketCase]]
     * for the acquisition and release of resources.
     *
     * @see [[guarantee]] for the simpler version
     * @see [[bracketCase]] for the more general operation
     */
    public final IO<A> guaranteeCase(Function1<ExitCase<Throwable>, IO<Unit>> finalizer) {
        return IOBracket.guaranteeCase(this, finalizer);
    }

    public final IO<A> handleErrorWith(Function1<Throwable, IO<A>> f) {
        return new Bind<>(this, new ErrorHandler<>(f));
    }

    public final <B> IO<B> redeem(Function1<Throwable, B> recover, Function1<A, B> map) {
        return new Bind<>(this, new Redeem<>(recover, map));
    }

    public final <B> IO<B> redeemWith(Function1<Throwable, IO<B>> recover, Function1<A, IO<B>> map) {
        return new Bind<>(this, new RedeemWith<>(recover, map));
    }

    public static <A> IO<A> apply(Supplier<A> thunk) {
        return delay(thunk);
    }

    public static <A> IO<A> delay(Supplier<A> thunk) {
        return new Delay<>(thunk);
    }

    public static <A> IO<A> suspend(Supplier<IO<A>> thunk) {
        return new Suspend<>(thunk);
    }

    public static <A> IO<A> pure(A a) {
        return new Pure<>(a);
    }

    public static IO<Unit> unit() {
        return new Pure<>(Unit.unit());
    }

    public static <A> IO<A> async(Consumer<Consumer<Either<Throwable, A>>> k) {
        return new Async<>((conn, cb) -> {
            var cb2 = Callback.asyncIdempotent(null, cb);
            try {
                k.accept(cb2);
            } catch (Exception e) {
                cb2.accept(Either.left(e));
            }
        });
    }

    public static <A> IO<A> asyncF(Function1<Consumer<Either<Throwable, A>>, IO<Unit>> k) {
        return new Async<>((conn, cb) -> {
            var conn2 = IOConnection.apply();
            var cb2 = Callback.asyncIdempotent(null, cb);
            conn.push(conn2.cancel());
            IO<Unit> fa;
            try {
                fa = k.apply(cb2);
            } catch (Exception e) {
                fa = IO.apply(() -> {
                    cb2.accept(Either.left(e));
                    return Unit.unit();
                });
            }
            IORunLoop.startCancelable(fa, conn2, Callback.report());
        });
    }

    public static <A> IO<A> cancelable(Function1<Consumer<Either<Throwable, A>>, IO<Unit>> k) {
        return new Async<>((conn, cb) -> {
            var cb2 = Callback.asyncIdempotent(null, cb);
            var ref = ForwardCancelable.apply();
            conn.push(ref.cancel());
            try {
                ref.set(k.apply(cb2));
            } catch (Exception e) {
                cb2.accept(Either.left(e));
                ref.set(IO.unit());
            }
        });
    }

    public static <A> IO<A> raiseError(Throwable e) {
        return new RaiseError<>(e);
    }

    public static <A> IO<A> fromFuture(CompletableFuture<A> promise) {
        return async(cb -> {
            promise.whenComplete((a, e) -> {
                if (e != null) {
                    cb.accept(Either.left(e));
                } else {
                    cb.accept(Either.right(a));
                }
            });
        });
    }

    public static <A> IO<A> fromFuture(IO<CompletableFuture<A>> iof) {
        return iof.flatMap(IO::fromFuture);
    }

    public static <A> IO<A> fromEither(Either<Throwable, A> e) {
        return e.fold(IO::raiseError, IO::pure);
    }

    public static IO<Unit> shift(ExecutionContext ec) {
        return shift(IOContextShift.apply(ec));
    }

    public static IO<Unit> shift(ContextShift<IO> cs) {
        return (IO<Unit>) cs.shift();
    }

    public static IO<Unit> sleep(Duration duration, Timer<IO> timer) {
        return (IO<Unit>) timer.sleep(duration);
    }

    public static IO<Unit> cancelBoundary() {
        return new Async<>((conn, cb) -> cb.accept(Either.right(Unit.unit())));
    }

    public static <A, B> IO<Either<A, B>> race(IO<A> lh, IO<B> rh, ContextShift<IO> cs) {
        return IORace.simple(cs, lh, rh);
    }

    /**
     * Run two IO tasks concurrently, and returns a pair
     * containing both the winner's successful value and the loser
     * represented as a still-unfinished task.
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
     * IO.racePair(ioA, ioB).flatMap {
     * case Left((a, fiberB)) =>
     * fiberB.cancel.map(_ => a)
     * case Right((fiberA, b)) =>
     * fiberA.cancel.map(_ => b)
     * }
     * }}}
     * <p>
     * N.B. this is the implementation of [[Concurrent.racePair]].
     * <p>
     * See [[race]] for a simpler version that cancels the loser
     * immediately.
     *
     * @param lh is the "left" task participating in the race
     * @param rh is the "right" task participating in the race
     * @param cs is an implicit requirement needed because
     *           `race` automatically forks the involved tasks
     */
    public static <A, B> IO<Either<Tuple2<A, Fiber<IO, B>>, Tuple2<Fiber<IO, A>, B>>> racePair(IO<A> lh, IO<B> rh, ContextShift<IO> cs) {
        return IORace.pair(cs, lh, rh);
    }

    public static Effect<IO> effect() {
        return IOEffect.instance;
    }

    /**
     * Returns a [[Timer]] instance for [[IO]], built from a
     * Scala `ExecutionContext`.
     * <p>
     * N.B. this is the JVM-specific version. On top of JavaScript
     * the implementation needs no `ExecutionContext`.
     *
     * @param ec is the execution context used for actual execution
     *           tasks (e.g. bind continuations)
     */
    public static Timer<IO> timer(ExecutionContext ec) {
        return IOTimer.apply(ec);
    }

    /**
     * Returns a [[Timer]] instance for [[IO]], built from a
     * Scala `ExecutionContext` and a Java `ScheduledExecutorService`.
     * <p>
     * N.B. this is the JVM-specific version. On top of JavaScript
     * the implementation needs no `ExecutionContext`.
     *
     * @param ec is the execution context used for actual execution
     *           tasks (e.g. bind continuations)
     * @param sc is the `ScheduledExecutorService` used for scheduling
     *           ticks with a delay
     */
    public static Timer<IO> timer(ExecutionContext ec, ScheduledExecutorService sc) {
        return IOTimer.apply(ec, sc);
    }

    public static final class Pure<A> extends IO<A> {
        private final A a;

        public Pure(A a) {
            this.a = a;
        }

        public A a() {
            return a;
        }
    }

    public static final class Delay<A> extends IO<A> {
        private final Supplier<A> thunk;

        public Delay(Supplier<A> thunk) {
            this.thunk = thunk;
        }

        public Supplier<A> thunk() {
            return thunk;
        }
    }

    public static final class RaiseError<A> extends IO<A> {
        private final Throwable e;

        public RaiseError(Throwable e) {
            this.e = e;
        }

        public Throwable e() {
            return e;
        }
    }

    public static final class Suspend<A> extends IO<A> {
        private final Supplier<IO<A>> thunk;

        public Suspend(Supplier<IO<A>> thunk) {
            this.thunk = thunk;
        }

        public Supplier<IO<A>> thunk() {
            return thunk;
        }
    }

    public static final class Bind<E, A> extends IO<A> {
        private final IO<E> source;
        private final Function1<E, IO<A>> f;

        public Bind(IO<E> source, Function1<E, IO<A>> f) {
            this.source = source;
            this.f = f;
        }

        public IO<E> source() {
            return source;
        }

        public Function1<E, IO<A>> f() {
            return f;
        }
    }

    public static final class Map<E, A> extends IO<A> implements Function1<E, IO<A>> {
        private final IO<E> source;
        private final Function1<E, A> f;
        private final int index;

        public Map(IO<E> source, Function1<E, A> f, int index) {
            this.source = source;
            this.f = f;
            this.index = index;
        }

        @Override
        public <B> IO<B> map(Function1<A, B> f) {
            // Allowed to do fixed number of map operations fused before
            // resetting the counter in order to avoid stack overflows;
            // See `IOPlatform` for details on this maximum.
            if (index != fusionMaxStackDepth) {
                return new Map<>(source, e -> f.apply(this.f.apply(e)), index + 1);
            } else {
                return new Map<>(this, f, 0);
            }
        }

        public IO<E> source() {
            return source;
        }

        public Function1<E, A> f() {
            return f;
        }

        public int index() {
            return index;
        }

        @Override
        public IO<A> apply(E e) {
            return IO.pure(f.apply(e));
        }
    }

    public static final class Async<A> extends IO<A> {
        private final BiConsumer<IOConnection, Consumer<Either<Throwable, A>>> k;
        private final boolean trampolineAfter;

        public Async(BiConsumer<IOConnection, Consumer<Either<Throwable, A>>> k) {
            this(k, false);
        }

        public Async(BiConsumer<IOConnection, Consumer<Either<Throwable, A>>> k, boolean trampolineAfter) {
            this.k = k;
            this.trampolineAfter = trampolineAfter;
        }

        public BiConsumer<IOConnection, Consumer<Either<Throwable, A>>> k() {
            return k;
        }

        public boolean trampolineAfter() {
            return trampolineAfter;
        }

    }

    public static final class ContextSwitch<A> extends IO<A> {
        private final IO<A> source;
        private final Function1<IOConnection, IOConnection> modify;
        private final Function4<A, Throwable, IOConnection, IOConnection, IOConnection> restore;

        public ContextSwitch(IO<A> source, Function1<IOConnection, IOConnection> modify, Function4<A, Throwable, IOConnection, IOConnection, IOConnection> restore) {
            this.source = source;
            this.modify = modify;
            this.restore = restore;
        }

        public IO<A> source() {
            return source;
        }

        public Function1<IOConnection, IOConnection> modify() {
            return modify;
        }

        public Function4<A, Throwable, IOConnection, IOConnection, IOConnection> restore() {
            return restore;
        }
    }

    private static class IOEffect implements Effect<IO>, StackSafeMonad<IO> {
        private final static Effect<IO> instance = new IOEffect();

        private IOEffect() {
        }

        @Override
        public <A> IO<A> pure(A a) {
            return IO.pure(a);
        }

        @Override
        public IO<Unit> unit() {
            return IO.unit();
        }

        @Override
        public <A, B> IO<B> map(Higher<IO, A> fa, Function1<A, B> f) {
            return ((IO<A>) fa).map(f);
        }

        @Override
        public <A, B> Higher<IO, B> flatMap(Higher<IO, A> fa, Function1<A, Higher<IO, B>> f) {
            return ((IO<A>) fa).flatMap(a -> (IO<B>) f.apply(a));
        }

        @Override
        public <A> IO<Either<Throwable, A>> attempt(Higher<IO, A> fa) {
            return ((IO<A>) fa).attempt();
        }

        @Override
        public <A> IO<A> handleErrorWith(Higher<IO, A> fa, Function1<Throwable, Higher<IO, A>> f) {
            return ((IO<A>) fa).handleErrorWith(e -> (IO<A>) f.apply(e));
        }

        @Override
        public <A> IO<A> raiseError(Throwable e) {
            return IO.raiseError(e);
        }

        @Override
        public <A, B> IO<B> bracket(Higher<IO, A> acquire,
                                    Function1<A, Higher<IO, B>> use,
                                    Function1<A, Higher<IO, Unit>> release) {
            return ((IO<A>) acquire).bracket(
                a -> (IO<B>) use.apply(a),
                a -> (IO<Unit>) release.apply(a)
            );
        }

        @Override
        public <A, B> IO<B> bracketCase(Higher<IO, A> acquire,
                                        Function1<A, Higher<IO, B>> use,
                                        Function2<A, ExitCase<Throwable>, Higher<IO, Unit>> release) {
            return ((IO<A>) acquire).bracketCase(
                a -> (IO<B>) use.apply(a),
                (a, ec) -> (IO<Unit>) release.apply(a, ec)
            );
        }

        @Override
        public <A> IO<A> uncancelable(Higher<IO, A> fa) {
            return ((IO<A>) fa).uncancelable();
        }

        @Override
        public <A> IO<A> guarantee(Higher<IO, A> fa, Higher<IO, Unit> finalizer) {
            return ((IO<A>) fa).guarantee((IO<Unit>) finalizer);
        }

        @Override
        public <A> IO<A> guaranteeCase(Higher<IO, A> fa, Function1<ExitCase<Throwable>, Higher<IO, Unit>> finalizer) {
            return ((IO<A>) fa).guaranteeCase(ec -> (IO<Unit>) finalizer.apply(ec));
        }

        @Override
        public <A> IO<A> delay(Supplier<A> thunk) {
            return IO.delay(thunk);
        }

        @Override
        public <A> IO<A> suspend(Supplier<Higher<IO, A>> thunk) {
            return IO.suspend(() -> (IO<A>) thunk.get());
        }

        @Override
        public <A> IO<A> async(Consumer<Consumer<Either<Throwable, A>>> k) {
            return IO.async(k);
        }

        @Override
        public <A> IO<A> asyncF(Function1<Consumer<Either<Throwable, A>>, Higher<IO, Unit>> k) {
            return IO.asyncF(cb -> (IO<Unit>) k.apply(cb));
        }

        @Override
        public <A> IO<A> liftIO(IO<A> ioa) {
            return ioa;
        }

        @Override
        public <A> IO<A> toIO(Higher<IO, A> fa) {
            return (IO<A>) fa;
        }

        @Override
        public <A> SyncIO<Unit> runAsync(Higher<IO, A> fa, Function1<Either<Throwable, A>, IO<Unit>> cb) {
            return ((IO<A>) fa).runAsync(cb);
        }
    }

    private static final class IOConcurrentEffect extends IOEffect implements ConcurrentEffect<IO> {
        private final ContextShift<IO> cs;

        private IOConcurrentEffect(ContextShift<IO> cs) {
            this.cs = cs;
        }

        @Override
        public <A> IO<Fiber<IO, A>> start(Higher<IO, A> fa) {
            return ((IO<A>) fa).start(cs);
        }

        @Override
        public <A, B> IO<Either<A, B>> race(Higher<IO, A> fa, Higher<IO, B> fb) {
            return IO.race((IO<A>) fa, (IO<B>) fb, cs);
        }

        @Override
        public <A, B> IO<Either<Tuple2<A, Fiber<IO, B>>, Tuple2<Fiber<IO, A>, B>>> racePair(Higher<IO, A> fa, Higher<IO, B> fb) {
            return IO.racePair((IO<A>) fa, (IO<B>) fb, cs);
        }

        @Override
        public <A> IO<A> cancelable(Function1<Consumer<Either<Throwable, A>>, Higher<IO, Unit>> k) {
            return IO.cancelable(e -> (IO<Unit>) k.apply(e));
        }

        @Override
        public <A> SyncIO<Higher<IO, Unit>> runCancelable(Higher<IO, A> fa, Function1<Either<Throwable, A>, IO<Unit>> cb) {
            return ((IO<A>) fa).runCancelable(cb).map(a -> a);
        }

        @Override
        public <A> IO<A> toIO(Higher<IO, A> fa) {
            return (IO<A>) fa;
        }

        @Override
        public <A> IO<A> liftIO(IO<A> ioa) {
            return ioa;
        }
    }
}