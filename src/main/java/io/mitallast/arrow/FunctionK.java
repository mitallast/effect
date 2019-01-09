package io.mitallast.arrow;

import io.mitallast.higher.Higher;
import io.mitallast.lambda.Function1;

/**
 * `FunctionK[F[_], G[_]]` is a functor transformation from `F` to `G`
 * in the same manner that function `A => B` is a morphism from values
 * of type `A` to `B`.
 * An easy way to create a FunctionK instance is to use the Polymorphic
 * lambdas provided by non/kind-projector v0.9+. E.g.
 * {{{
 * val listToOption = λ[FunctionK[List, Option]](_.headOption)
 * }}}
 */
public interface FunctionK<F extends Higher, G extends Higher> {

    /**
     * Applies this functor transformation from `F` to `G`
     */
    <A> Higher<G, A> apply(Higher<F, A> fa);

    /**
     * Composes two instances of FunctionK into a new FunctionK with this
     * transformation applied last.
     */
    default <E extends Higher> FunctionK<E, G> compose(FunctionK<E, F> f) {
        var self = this;
        return new FunctionK<>() {
            @Override
            public <A> Higher<G, A> apply(Higher<E, A> fa) {
                return self.apply(f.apply(fa));
            }
        };
    }

    /**
     * Composes two instances of FunctionK into a new FunctionK with this
     * transformation applied first.
     */
    default <H extends Higher> FunctionK<F, H> andThen(FunctionK<G, H> f) {
        return f.compose(this);
    }

    /**
     * The identity transformation of `F` to `F`
     */
    static <F extends Higher> FunctionK<F, F> id() {
        return new FunctionK<>() {
            @Override
            public <A> Higher<F, A> apply(Higher<F, A> fa) {
                return fa;
            }
        };
    }

    /**
     * Lifts function `f` of `F[A] => G[A]` into a `FunctionK[F, G]`.
     * <p>
     * {{{
     * def headOption[A](list: List[A]): Option[A] = list.headOption
     * val lifted: FunctionK[List, Option] = FunctionK.lift(headOption)
     * }}}
     */
    static <F extends Higher, G extends Higher>
    FunctionK<F, G> lift(Function1<Higher<F, ?>, Higher<G, ?>> f) {
        return new FunctionK<>() {
            @SuppressWarnings("unchecked")
            @Override
            public <A> Higher<G, A> apply(Higher<F, A> fa) {
                return (Higher<G, A>) f.apply(fa);
            }
        };
    }
}
