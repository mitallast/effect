package io.mitallast;

import io.mitallast.arrow.FunctionK;
import io.mitallast.free.Free;
import io.mitallast.higher.Higher;
import io.mitallast.kernel.Id;
import io.mitallast.kernel.Unit;
import io.mitallast.lambda.Function1;
import io.mitallast.maybe.Maybe;

import java.util.HashMap;
import java.util.Map;

import static io.mitallast.free.Free.liftF;
import static io.mitallast.kernel.Unit.unit;

public class FreeExample {

    static abstract class KVStoreA<A> implements Higher<KVStoreA, A> {
    }

    static final class Put<T> extends KVStoreA<Unit> {
        private final String key;
        private final T value;

        Put(String key, T value) {
            this.key = key;
            this.value = value;
        }
    }

    static final class Get<T> extends KVStoreA<Maybe<T>> {
        private final String key;

        Get(String key) {
            this.key = key;
        }
    }

    static final class Delete extends KVStoreA<Unit> {
        private final String key;

        Delete(String key) {
            this.key = key;
        }
    }


    static final class FreeStore {
        public static <T> Free<KVStoreA, Unit> put(String key, T value) {
            return liftF(new Put<>(key, value));
        }

        public static <T> Free<KVStoreA, Maybe<T>> get(String key) {
            return liftF(new Get<>(key));
        }

        public static Free<KVStoreA, Unit> delete(String key) {
            return liftF(new Delete(key));
        }

        public static <T> Free<KVStoreA, Unit> update(String key, Function1<T, T> f) {
            return FreeStore.<T>get(key).flatMap(vMaybe -> vMaybe.map(v -> put(key, f.apply(v))).getOrElse(Free.pure(unit())));
        }


        public static Free<KVStoreA, Maybe<Integer>> program() {
            return put("wild-cats", 2)
                .flatMap(u -> FreeStore.<Integer>update("wild-cats", i -> i + 1))
                .flatMap(u -> put("tame-cats", 5))
                .flatMap(u -> FreeStore.<Integer>get("wild-cats"))
                .flatMap(n -> delete("tame-cats").map(u -> n));
        }

        static FunctionK<KVStoreA, Id> impureCompiler() {
            return new FunctionK<>() {
                private Map<String, Object> kvs = new HashMap<>();

                @Override
                public <A> Id<A> apply(Higher<KVStoreA, A> fa) {
                    if (fa instanceof Put) {
                        var put = (Put<A>) fa;
                        kvs.put(put.key, put.value);
                        return Id.apply((A) unit());
                    } else if (fa instanceof Get) {
                        var get = (Get<A>) fa;
                        return Id.apply((A) Maybe.apply(kvs.get(get.key)));
                    } else if (fa instanceof Delete) {
                        var delete = (Delete) fa;
                        kvs.remove(delete.key);
                        return Id.apply((A) unit());
                    } else throw new IllegalArgumentException();
                }
            };
        }

        static void runImpureCompiler() {
            Id<Maybe<Integer>> id = (Id<Maybe<Integer>>) program().foldMap(impureCompiler(), Id.instances());
            Maybe<Integer> result = id.value();

            System.out.println("impure compiler:");
            System.out.println(result);
        }

    }

    public static void main(String... args) {
        FreeStore.runImpureCompiler();
    }
}
