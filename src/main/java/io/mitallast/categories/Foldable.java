package io.mitallast.categories;

import io.mitallast.either.Either;
import io.mitallast.higher.Higher;
import io.mitallast.kernel.CommutativeMonoid;
import io.mitallast.kernel.Eval;
import io.mitallast.kernel.Monoid;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;
import io.mitallast.lambda.Function2;
import io.mitallast.maybe.Maybe;
import io.mitallast.product.Tuple1;
import io.mitallast.product.Tuple2;

public interface Foldable<F extends Higher> extends UnorderedFoldable<F> {
    <A, B> B foldLeft(Higher<F, A> fa, B b, Function2<B, A, B> f);

    <A, B> Eval<B> foldRight(Higher<F, A> fa, Eval<B> lb, Function2<A, Eval<B>, Eval<B>> f);

    /**
     * Fold implemented using the given Monoid[A] instance.
     */
    default <A> A fold(Higher<F, A> fa, Monoid<A> A) {
        return foldLeft(fa, A.empty(), A::combine);
    }

    /**
     * Alias for [[fold]].
     */
    default <A> A combineAll(Higher<F, A> fa, Monoid<A> A) {
        return fold(fa, A);
    }

    /**
     * Fold implemented by mapping `A` values into `B` and then
     * combining them using the given `Monoid[B]` instance.
     */
    default <A, B> B foldMap(Higher<F, A> fa, Function1<A, B> f, Monoid<B> B) {
        return foldLeft(fa, B.empty(), (b, a) -> B.combine(b, f.apply(a)));
    }

    /**
     * Perform a stack-safe monadic left fold from the source context `F`
     * into the target monad `G`.
     * <p>
     * This method can express short-circuiting semantics. Even when
     * `fa` is an infinite structure, this method can potentially
     * terminate if the `foldRight` implementation for `F` and the
     * `tailRecM` implementation for `G` are sufficiently lazy.
     * <p>
     * Instances for concrete structures (e.g. `List`) will often
     * have a more efficient implementation than the default one
     * in terms of `foldRight`.
     */
    default <G extends Higher, A, B> Higher<G, B> foldM(Higher<F, A> fa,
                                                        B z,
                                                        Function2<B, A, Higher<G, B>> f,
                                                        Monad<G> G) {

        var src = Source.fromFoldable(fa, this);
        return G.tailRecM(new Tuple2<>(z, src), tuple -> {
            var b = tuple.t1();
            var srcB = tuple.t2();
            var unc = srcB.uncons();
            if (unc.isDefined()) {
                var tuple2 = unc.get();
                var a = tuple2.t1();
                var srcA = tuple2.t2();
                return G.map(f.apply(b, a), bb -> Either.left(new Tuple2<>(bb, srcA.value())));
            } else {
                return G.pure(Either.right(b));
            }
        });
    }

    /**
     * Alias for [[foldM]].
     */
    default <G extends Higher, A, B> Higher<G, B> foldLeftM(Higher<F, A> fa,
                                                            B z,
                                                            Function2<B, A, Higher<G, B>> f,
                                                            Monad<G> G) {
        return foldM(fa, z, f, G);
    }

    /**
     * Monadic folding on `F` by mapping `A` values to `G[B]`, combining the `B`
     * values using the given `Monoid[B]` instance.
     * <p>
     * Similar to [[foldM]], but using a `Monoid[B]`.
     * <p>
     * {{{
     * scala> import cats.Foldable
     * scala> import cats.implicits._
     * scala> val evenNumbers = List(2,4,6,8,10)
     * scala> val evenOpt: Int => Option[Int] =
     * |   i => if (i % 2 == 0) Some(i) else None
     * scala> Foldable[List].foldMapM(evenNumbers)(evenOpt)
     * res0: Option[Int] = Some(30)
     * scala> Foldable[List].foldMapM(evenNumbers :+ 11)(evenOpt)
     * res1: Option[Int] = None
     * }}}
     */
    default <G extends Higher, A, B> Higher<G, B> foldMapM(Higher<F, A> fa,
                                                           Function1<A, Higher<G, B>> f,
                                                           Monad<G> G,
                                                           Monoid<B> B) {
        return foldM(fa, B.empty(), (b, a) -> G.map(f.apply(a), bb -> B.combine(b, bb)), G);
    }


    @Override
    default <A, B> B unorderedFoldMap(Higher<F, A> fa, Function1<A, B> f, CommutativeMonoid<B> B) {
        return foldMap(fa, f, B);
    }

    @Override
    default <A> A unorderedFold(Higher<F, A> fa, CommutativeMonoid<A> A) {
        return fold(fa, A);
    }
}


abstract class Source<A> {
    static <A> Source<A> empty() {
        return new Source<>() {
            @Override
            Maybe<Tuple2<A, Eval<Source<A>>>> uncons() {
                return Maybe.none();
            }
        };
    }

    abstract Maybe<Tuple2<A, Eval<Source<A>>>> uncons();

    static <A> Source<A> cons(A a, Eval<Source<A>> src) {
        return new Source<>() {
            @Override
            Maybe<Tuple2<A, Eval<Source<A>>>> uncons() {
                return Maybe.some(new Tuple2<>(a, src));
            }
        };
    }

    static <F extends Higher, A> Source<A> fromFoldable(Higher<F, A> fa, Foldable<F> F) {
        return F.foldRight(fa, Eval.now(empty()),
            (Function2<A, Eval<Source<A>>, Eval<Source<A>>>) (a, evalSrc) -> Eval.later(() -> cons(a, evalSrc)))
            .value();
    }
}