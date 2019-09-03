package io.mitallast.higher;

public interface Higher<H extends Higher, T> {
    @SuppressWarnings("unchecked")
    default <TT> Higher<H, TT> castTUnsafe() {
        return (Higher<H, TT>) this;
    }
}