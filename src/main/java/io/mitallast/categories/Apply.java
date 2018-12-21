package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;
import io.mitallast.product.Tuple2;

import java.util.function.BiFunction;

public interface Apply<F extends Higher> extends Functor<F>, Semigroupal<F> {
    <A, B> Higher<F, B> ap(Higher<F, Function1<A, B>> ff, Higher<F, A> fa);

    /**
     * Compose two actions, discarding any value produced by the first.
     *
     * @see [[productL]] to discard the value of the second instead.
     * <p>
     * Example:
     * {{{
     * scala> import cats.implicits._
     * scala> import cats.data.Validated
     * scala> import Validated.{Valid, Invalid}
     * <p>
     * scala> type ErrOr[A] = Validated[String, A]
     * <p>
     * scala> val validInt: ErrOr[Int] = Valid(3)
     * scala> val validBool: ErrOr[Boolean] = Valid(true)
     * scala> val invalidInt: ErrOr[Int] = Invalid("Invalid int.")
     * scala> val invalidBool: ErrOr[Boolean] = Invalid("Invalid boolean.")
     * <p>
     * scala> Apply[ErrOr].productR(validInt)(validBool)
     * res0: ErrOr[Boolean] = Valid(true)
     * <p>
     * scala> Apply[ErrOr].productR(invalidInt)(validBool)
     * res1: ErrOr[Boolean] = Invalid(Invalid int.)
     * <p>
     * scala> Apply[ErrOr].productR(validInt)(invalidBool)
     * res2: ErrOr[Boolean] = Invalid(Invalid boolean.)
     * <p>
     * scala> Apply[ErrOr].productR(invalidInt)(invalidBool)
     * res3: ErrOr[Boolean] = Invalid(Invalid int.Invalid boolean.)
     * }}}
     */
    default <A, B> Higher<F, B> productR(Higher<F, A> fa, Higher<F, B> fb) {
        return map2(fa, fb, (a, b) -> b);
    }

    /**
     * Compose two actions, discarding any value produced by the second.
     *
     * @see [[productR]] to discard the value of the first instead.
     * <p>
     * Example:
     * {{{
     * scala> import cats.implicits._
     * scala> import cats.data.Validated
     * scala> import Validated.{Valid, Invalid}
     * <p>
     * scala> type ErrOr[A] = Validated[String, A]
     * <p>
     * scala> val validInt: ErrOr[Int] = Valid(3)
     * scala> val validBool: ErrOr[Boolean] = Valid(true)
     * scala> val invalidInt: ErrOr[Int] = Invalid("Invalid int.")
     * scala> val invalidBool: ErrOr[Boolean] = Invalid("Invalid boolean.")
     * <p>
     * scala> Apply[ErrOr].productL(validInt)(validBool)
     * res0: ErrOr[Int] = Valid(3)
     * <p>
     * scala> Apply[ErrOr].productL(invalidInt)(validBool)
     * res1: ErrOr[Int] = Invalid(Invalid int.)
     * <p>
     * scala> Apply[ErrOr].productL(validInt)(invalidBool)
     * res2: ErrOr[Int] = Invalid(Invalid boolean.)
     * <p>
     * scala> Apply[ErrOr].productL(invalidInt)(invalidBool)
     * res3: ErrOr[Int] = Invalid(Invalid int.Invalid boolean.)
     * }}}
     */
    default <A, B> Higher<F, A> productL(Higher<F, A> fa, Higher<F, B> fb) {
        return map2(fa, fb, (a, b) -> a);
    }

    @Override
    default <A, B> Higher<F, Tuple2<A, B>> product(Higher<F, A> fa, Higher<F, B> fb) {
        return ap(map(fa, a -> b -> new Tuple2<>(a, b)), fb);
    }

    /**
     * Applies the pure (binary) function f to the effectful values fa and fb.
     * <p>
     * map2 can be seen as a binary version of [[cats.Functor]]#map.
     * <p>
     * Example:
     * {{{
     * scala> import cats.implicits._
     * <p>
     * scala> val someInt: Option[Int] = Some(3)
     * scala> val noneInt: Option[Int] = None
     * scala> val someLong: Option[Long] = Some(4L)
     * scala> val noneLong: Option[Long] = None
     * <p>
     * scala> Apply[Option].map2(someInt, someLong)((i, l) => i.toString + l.toString)
     * res0: Option[String] = Some(34)
     * <p>
     * scala> Apply[Option].map2(someInt, noneLong)((i, l) => i.toString + l.toString)
     * res0: Option[String] = None
     * <p>
     * scala> Apply[Option].map2(noneInt, noneLong)((i, l) => i.toString + l.toString)
     * res0: Option[String] = None
     * <p>
     * scala> Apply[Option].map2(noneInt, someLong)((i, l) => i.toString + l.toString)
     * res0: Option[String] = None
     * }}}
     */
    default <A, B, Z> Higher<F, Z> map2(Higher<F, A> fa, Higher<F, B> fb, BiFunction<A, B, Z> f) {
        return map(product(fa, fb), ab -> f.apply(ab.t1(), ab.t2()));
    }
}
