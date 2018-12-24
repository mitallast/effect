package io.mitallast.lambda;

@FunctionalInterface
public interface Supplier<T> {
    T get();

    @SuppressWarnings("unchecked")
    default <UT extends T> Supplier<UT> cast() {
        return (Supplier<UT>) this;
    }
}
