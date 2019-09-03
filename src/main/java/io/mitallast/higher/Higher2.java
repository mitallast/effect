package io.mitallast.higher;

public interface Higher2<H extends Higher2, A, B> extends Higher<Higher2<H, A, ?>, B> {
}