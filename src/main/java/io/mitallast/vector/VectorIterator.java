package io.mitallast.vector;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class VectorIterator<A> extends VectorPointer<A> implements Iterator<A> {
    private final int startIndex;
    private final int endIndex;

    private int blockIndex;
    private int lo;
    private int endLo;

    private boolean hasNext;

    VectorIterator(final int startIndex, final int endIndex) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.blockIndex = startIndex & ~31;
        this.lo = startIndex & 31;
        this.endLo = Math.min(32, endIndex - blockIndex);
        this.hasNext = blockIndex + lo < endIndex;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public A next() {
        if (!hasNext) {
            throw new NoSuchElementException();
        }

        var res = (A) display0[lo];
        lo++;
        if (lo == endLo) {
            if (blockIndex + lo < endIndex) {
                var newBlockIndex = blockIndex + 32;
                gotoNextBlockStart(newBlockIndex, blockIndex ^ newBlockIndex);

                blockIndex = newBlockIndex;
                endLo = Math.min(endIndex - blockIndex, 32);
                lo = 0;
            } else {
                hasNext = false;
            }
        }
        return res;
    }
}
