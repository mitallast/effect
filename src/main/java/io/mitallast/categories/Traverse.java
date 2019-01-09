package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

public interface Traverse<F extends Higher> extends Functor<F>, Foldable<F>, UnorderedTraverse<F> {
    <G extends Higher, A, B>
    Higher<G, Higher<F, B>> traverse(Higher<F, A> fa, Function1<A, Higher<G, B>> f, Applicative<G> G);

    default <G extends Higher, A, B>
    Higher<G, Higher<F, B>> flatTraverse(Higher<F, A> fa,
                                         Function1<A, Higher<G, Higher<F, B>>> f,
                                         Applicative<G> G,
                                         FlatMap<F> F) {
        return G.map(traverse(fa, f, G), F::flatten);
    }

    /**
     * Thread all the G effects through the F structure to invert the
     * structure from F[G[A]] to G[F[A]].
     * <p>
     * Example:
     * {{{
     * scala> import cats.implicits._
     * scala> val x: List[Option[Int]] = List(Some(1), Some(2))
     * scala> val y: List[Option[Int]] = List(None, Some(2))
     * scala> x.sequence
     * res0: Option[List[Int]] = Some(List(1, 2))
     * scala> y.sequence
     * res1: Option[List[Int]] = None
     * }}}
     */
    default <G extends Higher, A> Higher<G, Higher<F, A>> sequence(Higher<F, Higher<G, A>> fga, Applicative<G> G) {
        return traverse(fga, ga -> ga, G);
    }

    /**
     * Thread all the G effects through the F structure and flatten to invert the
     * structure from F[G[F[A]]] to G[F[A]].
     * <p>
     * Example:
     * {{{
     * scala> import cats.implicits._
     * scala> val x: List[Option[List[Int]]] = List(Some(List(1, 2)), Some(List(3)))
     * scala> val y: List[Option[List[Int]]] = List(None, Some(List(3)))
     * scala> x.flatSequence
     * res0: Option[List[Int]] = Some(List(1, 2, 3))
     * scala> y.flatSequence
     * res1: Option[List[Int]] = None
     * }}}
     */
    default <G extends Higher, A> Higher<G, Higher<F, A>> flatSequence(
        Higher<F, Higher<G, Higher<F, A>>> fgfa,
        Applicative<G> G,
        FlatMap<F> F
    ) {
        return G.map(sequence(fgfa, G), F::flatten);
    }

    @Override
    default <G extends Higher, A, B> Higher<G, Higher<F, B>> unorderedTraverse(Higher<F, A> sa,
                                                                               Function1<A, Higher<G, B>> f,
                                                                               CommutativeApplicative<G> G) {
        return traverse(sa, f, G);
    }

    @Override
    default <G extends Higher, A> Higher<G, Higher<F, A>> unorderedSequence(Higher<F, Higher<G, A>> fga,
                                                                            CommutativeApplicative<G> G) {
        return sequence(fga, G);
    }
}
