package io.mitallast.vector;

public class VectorBuilder<A> extends VectorPointer<A> {

    // possible alternative: start with display0 = null, blockIndex = -32, lo = 32
    // to avoid allocating initial array if the result will be empty anyways

    private int blockIndex = 0;
    private int lo = 0;

    VectorBuilder() {
        display0 = new Object[32];
        depth = 1;
    }

    public VectorBuilder<A> append(A elem) {
        if (lo >= display0.length) {
            var newBlockIndex = blockIndex + 32;
            gotoNextBlockStartWritable(newBlockIndex, blockIndex ^ newBlockIndex);
            blockIndex = newBlockIndex;
            lo = 0;
        }
        display0[lo] = elem;
        lo += 1;
        return this;
    }

    public VectorBuilder<A> appendAll(Iterable<A> xs) {
        for (A x : xs) {
            append(x);
        }
        return this;
    }

    public Vector<A> result() {
        var size = blockIndex + lo;
        if (size == 0)
            return Vector.empty();
        var s = new Vector<A>(0, size, 0); // should focus front or back?
        s.initFrom(this);
        if (depth > 1) s.gotoPos(0, size - 1); // we're currently focused to size - 1, not size!
        return s;
    }

    public void clear() {
        display0 = new Object[32];
        depth = 1;
        blockIndex = 0;
        lo = 0;
    }
}
