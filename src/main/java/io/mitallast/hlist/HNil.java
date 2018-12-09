package io.mitallast.hlist;

public final class HNil implements HList {

    public <H> HCons<H, HNil> prepend(H h) {
        return new HCons<>(h, this) {
        };
    }

    @Override
    public String toString() {
        return "HNil";
    }
}
