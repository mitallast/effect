package io.mitallast.categories;

import io.mitallast.higher.Higher;
import io.mitallast.kernel.Id;
import io.mitallast.lambda.Function1;
import io.mitallast.maybe.Maybe;

public interface TraverseFilter<F extends Higher> extends FunctorFilter<F> {
//    Traverse<F> traverse();

//    @Override
//    Functor<F> functor();

    <G extends Higher, A, B> Higher<G, Higher<F, B>> traverseFilter(Higher<F, A> fa,
                                                                    Function1<A, Higher<G, Maybe<B>>> f,
                                                                    Applicative<G> G);

    default <G extends Higher, A> Higher<G, Higher<F, A>> filterA(Higher<F, A> fa,
                                                                  Function1<A, Higher<G, Boolean>> f,
                                                                  Applicative<G> G) {
        return traverseFilter(fa, a -> G.map(f.apply(a), i -> i ? Maybe.some(a) : Maybe.none()), G);
    }

    @Override
    default <A, B> Higher<F, B> mapFilter(Higher<F, A> fa, Function1<A, Maybe<B>> f) {
        var id = this.traverseFilter(
            fa,
            a -> Id.apply(f.apply(a)),
            Id.instances()
        );
        return ((Id<Higher<F, B>>) id).value();
    }
}
