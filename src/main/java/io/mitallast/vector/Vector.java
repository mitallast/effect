package io.mitallast.vector;

import io.mitallast.higher.Higher;
import io.mitallast.product.Tuple;
import io.mitallast.product.Tuple2;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class Vector<A> extends VectorPointer<A> implements Iterable<A>, Higher<Vector, A> {
    private final int startIndex;
    private final int endIndex;
    private final int focus;

    private boolean dirty = false;

    Vector(final int startIndex, final int endIndex, final int focus) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.focus = focus;
    }

    public int size() {
        return endIndex - startIndex;
    }

    public boolean isEmpty() {
        return endIndex > startIndex;
    }

    <B extends A> void initIterator(VectorIterator<B> s) {
        s.initFrom(this);
        if (dirty) s.stabilize(focus);
        if (s.depth > 1) s.gotoPos(startIndex, startIndex ^ focus);
    }

    @Override
    public Iterator<A> iterator() {
        var s = new VectorIterator<A>(startIndex, endIndex);
        initIterator(s);
        return s;
    }

    public Iterator<A> reverseIterator() {
        return new Iterator<A>() {
            int i = size();

            @Override
            public boolean hasNext() {
                return 0 < i;
            }

            @Override
            public A next() {
                if (0 < i) {
                    i -= 1;
                    return apply(i);
                } else throw new NoSuchElementException();
            }
        };
    }

    public A apply(int index) {
        var idx = checkRangeConvert(index);
        return getElem(idx, idx ^ focus);
    }

    private int checkRangeConvert(int index) {
        var idx = index + startIndex;
        if (index >= 0 && idx < endIndex) {
            return idx;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public <B extends A> Vector<B> updated(int index, B elem) {
        var idx = checkRangeConvert(index);
        var s = new Vector<B>(startIndex, endIndex, idx);
        s.initFrom(this);
        s.dirty = dirty;
        s.gotoPosWritable(focus, idx, focus ^ idx);  // if dirty commit changes; go to new pos and prepare for writing
        s.display0[idx & 31] = elem;
        return s;
    }

    // appendFront
    public <B extends A> Vector<B> prepend(B value) {
        if (endIndex != startIndex) {
            var blockIndex = (startIndex - 1) & ~31;
            var lo = (startIndex - 1) & 31;
            if (startIndex != blockIndex + 32) {
                var s = new Vector<B>(startIndex - 1, endIndex, blockIndex);
                s.initFrom(this);
                s.dirty = dirty;
                s.gotoPosWritable(focus, blockIndex, focus ^ blockIndex);
                s.display0[lo] = value;
                return s;
            } else {
                var freeSpace = (1 << (5 * depth)) - endIndex;     // free space at the right given the current tree-structure depth
                var shift = freeSpace & -(1 << (5 * (depth - 1))); // number of elements by which we'll shift right (only move at top level)
                var shiftBlocks = freeSpace >>> (5 * (depth - 1)); // number of top-level blocks

                if (shift != 0) {
                    // case A: we can shift right on the top level
                    if (depth > 1) {
                        var newBlockIndex = blockIndex + shift;
                        var newFocus = focus + shift;

                        var s = new Vector<B>(startIndex - 1 + shift, endIndex + shift, newBlockIndex);
                        s.initFrom(this);
                        s.dirty = dirty;
                        s.shiftTopLevel(0, shiftBlocks); // shift right by n blocks
                        s.gotoFreshPosWritable(newFocus, newBlockIndex, newFocus ^ newBlockIndex); // maybe create pos; prepare for writing
                        s.display0[lo] = value;
                        return s;
                    } else {
                        var newBlockIndex = blockIndex + 32;
                        var newFocus = focus;

                        var s = new Vector<B>(startIndex - 1 + shift, endIndex + shift, newBlockIndex);
                        s.initFrom(this);
                        s.dirty = dirty;
                        s.shiftTopLevel(0, shiftBlocks); // shift right by n blocks
                        s.gotoPosWritable(newFocus, newBlockIndex, newFocus ^ newBlockIndex); // prepare for writing
                        s.display0[shift - 1] = value;
                        return s;
                    }
                } else if (blockIndex < 0) {
                    // case B: we need to move the whole structure
                    var move = (1 << (5 * (depth + 1))) - (1 << (5 * depth));
                    var newBlockIndex = blockIndex + move;
                    var newFocus = focus + move;

                    var s = new Vector<B>(startIndex - 1 + move, endIndex + move, newBlockIndex);
                    s.initFrom(this);
                    s.dirty = dirty;
                    s.gotoFreshPosWritable(newFocus, newBlockIndex, newFocus ^ newBlockIndex);// could optimize: we know it will create a whole branch
                    s.display0[lo] = value;
                    return s;
                } else {
                    var newBlockIndex = blockIndex;
                    var newFocus = focus;

                    var s = new Vector<B>(startIndex - 1, endIndex, newBlockIndex);
                    s.initFrom(this);
                    s.dirty = dirty;
                    s.gotoFreshPosWritable(newFocus, newBlockIndex, newFocus ^ newBlockIndex);
                    s.display0[lo] = value;
                    return s;
                }
            }
        } else {
            // empty vector, just insert single element at the back
            var elems = new Object[32];
            elems[31] = value;
            var s = new Vector<B>(31, 32, 0);
            s.depth = 1;
            s.display0 = elems;
            return s;
        }
    }

    // appendBack
    public <B extends A> Vector<B> append(B value) {
        if (endIndex != startIndex) {
            var blockIndex = endIndex & ~31;
            var lo = endIndex & 31;

            if (endIndex != blockIndex) {
                var s = new Vector<B>(startIndex, endIndex + 1, blockIndex);
                s.initFrom(this);
                s.dirty = dirty;
                s.gotoPosWritable(focus, blockIndex, focus ^ blockIndex);
                s.display0[lo] = value;
                return s;
            } else {
                var shift = startIndex & -(1 << (5 * (depth - 1)));
                var shiftBlocks = startIndex >>> (5 * (depth - 1));

                if (shift != 0) {
                    if (depth > 1) {
                        var newBlockIndex = blockIndex - shift;
                        var newFocus = focus - shift;

                        var s = new Vector<B>(startIndex - shift, endIndex + 1 - shift, newBlockIndex);
                        s.initFrom(this);
                        s.dirty = dirty;
                        s.shiftTopLevel(shiftBlocks, 0); // shift left by n blocks
                        s.gotoFreshPosWritable(newFocus, newBlockIndex, newFocus ^ newBlockIndex);
                        s.display0[lo] = value;
                        return s;
                    } else {
                        var newBlockIndex = blockIndex - 32;
                        var newFocus = focus;

                        var s = new Vector<B>(startIndex - shift, endIndex + 1 - shift, newBlockIndex);
                        s.initFrom(this);
                        s.dirty = dirty;
                        s.shiftTopLevel(shiftBlocks, 0); // shift right by n elements
                        s.gotoPosWritable(newFocus, newBlockIndex, newFocus ^ newBlockIndex);
                        s.display0[32 - shift] = value;
                        return s;
                    }
                } else {
                    var newBlockIndex = blockIndex;
                    var newFocus = focus;

                    var s = new Vector<B>(startIndex, endIndex + 1, newBlockIndex);
                    s.initFrom(this);
                    s.dirty = dirty;
                    s.gotoFreshPosWritable(newFocus, newBlockIndex, newFocus ^ newBlockIndex);
                    s.display0[lo] = value;
                    return s;
                }
            }
        } else {
            var elems = new Object[32];
            elems[0] = value;
            var s = new Vector<B>(0, 1, 0);
            s.depth = 1;
            s.display0 = elems;
            return s;
        }
    }

    @SuppressWarnings("unchecked")
    public <B extends A> Vector<B> appendAll(Collection<B> that) {
        if (that.isEmpty()) {
            return (Vector<B>) this;
        } else {
            var size = that.size();
            if (size < TinyAppendFaster || size < (this.size() >>> Log2ConcatFaster)) {
                var v = (Vector<B>) this;
                for (var x : that) {
                    v = v.append(x);
                }
                return v;
//            } else if (this.size() < (size >>> Log2ConcatFaster) && that instanceof Vector) {
//                var v = (Vector<B>) that;
//                var ri = reverseIterator();
//                while (ri.hasNext()) {
//                    v = v.prepend((B) ri.next());
//                }
//                return v;
            } else {
                var builder = Vector.<B>builder();
                builder.appendAll((Vector<B>) this);
                builder.appendAll(that);
                return builder.result();
            }
        }
    }

    public Vector<A> take(int n) {
        if (n <= 0) {
            return Vector.empty();
        } else if (startIndex < endIndex - n) {
            return dropBack0(startIndex + n);
        } else {
            return this;
        }
    }

    public Vector<A> drop(int n) {
        if (n <= 0) {
            return this;
        } else if (startIndex < endIndex - n) {
            return dropFront0(startIndex + n);
        } else {
            return Vector.empty();
        }
    }

    public Vector<A> takeRight(int n) {
        if (n <= 0) {
            return Vector.empty();
        } else if (endIndex - n > startIndex) {
            return dropFront0(endIndex - n);
        } else {
            return this;
        }
    }

    public Vector<A> dropRight(int n) {
        if (n <= 0) {
            return this;
        } else if (endIndex - n > startIndex) {
            return dropBack0(endIndex - n);
        } else {
            return Vector.empty();
        }
    }

    public A head() {
        if (isEmpty()) throw new UnsupportedOperationException();
        return apply(0);
    }

    public Vector<A> tail() {
        if (isEmpty()) throw new UnsupportedOperationException();
        return drop(1);
    }

    public A last() {
        if (isEmpty()) throw new UnsupportedOperationException();
        return apply(size() - 1);
    }

    public Vector<A> init() {
        if (isEmpty()) throw new UnsupportedOperationException();
        return dropRight(1);
    }

    public Vector<A> slice(int from, int until) {
        return take(until).drop(from);
    }

    public Tuple2<Vector<A>, Vector<A>> splitAt(int n) {
        return Tuple.of(take(n), drop(n));
    }

    // low-level implementation (needs cleanup, maybe move to util class)

    private void shiftTopLevel(int oldLeft, int newLeft) {
        switch (depth - 1) {
            case 0:
                display0 = copyRange(display0, oldLeft, newLeft);
                break;
            case 1:
                display1 = copyRange(display1, oldLeft, newLeft);
                break;
            case 2:
                display2 = copyRange(display2, oldLeft, newLeft);
                break;
            case 3:
                display3 = copyRange(display3, oldLeft, newLeft);
                break;
            case 4:
                display4 = copyRange(display4, oldLeft, newLeft);
                break;
            case 5:
                display5 = copyRange(display5, oldLeft, newLeft);
                break;
        }
    }

    private void zeroLeft(Object[] array, int index) {
        var i = 0;
        while (i < index) {
            array[i] = null;
            i += 1;
        }
    }

    private void zeroRight(Object[] array, int index) {
        var i = index;
        while (i < array.length) {
            array[i] = null;
            i += 1;
        }
    }

    private Object[] copyLeft(Object[] array, int right) {
        var copy = new Object[array.length];
        System.arraycopy(array, 0, copy, 0, right);
        return copy;
    }

    private Object[] copyRight(Object[] array, int left) {
        var copy = new Object[array.length];
        System.arraycopy(array, left, copy, left, copy.length - left);
        return copy;
    }

    private void gotoPosWritable(int oldIndex, int newIndex, int xor) {
        if (dirty) {
            gotoPosWritable1(oldIndex, newIndex, xor);
        } else {
            gotoPosWritable0(newIndex, xor);
        }
    }

    private void gotoFreshPosWritable(int oldIndex, int newIndex, int xor) {
        if (dirty) {
            gotoFreshPosWritable1(oldIndex, newIndex, xor);
        } else {
            gotoFreshPosWritable0(oldIndex, newIndex, xor);
            dirty = true;
        }
    }

    private void preClean(int depth) {
        this.depth = depth;
        switch (depth - 1) {
            case 0:
                display1 = null;
                display2 = null;
                display3 = null;
                display4 = null;
                display5 = null;
                break;
            case 1:
                display2 = null;
                display3 = null;
                display4 = null;
                display5 = null;
                break;
            case 2:
                display3 = null;
                display4 = null;
                display5 = null;
                break;
            case 3:
                display4 = null;
                display5 = null;
                break;
            case 4:
                display5 = null;
                break;
            case 5:
                break;
        }
    }

    // requires structure is at index cutIndex and writable at level 0
    private void cleanLeftEdge(int cutIndex) {
        if (cutIndex < (1 << 5)) {
            zeroLeft(display0, cutIndex);
        } else if (cutIndex < (1 << 10)) {
            zeroLeft(display0, cutIndex & 31);
            display1 = copyRight(display1, cutIndex >>> 5);
        } else if (cutIndex < (1 << 15)) {
            zeroLeft(display0, cutIndex & 31);
            display1 = copyRight(display1, (cutIndex >>> 5) & 31);
            display2 = copyRight(display2, cutIndex >>> 10);
        } else if (cutIndex < (1 << 20)) {
            zeroLeft(display0, cutIndex & 31);
            display1 = copyRight(display1, (cutIndex >>> 5) & 31);
            display2 = copyRight(display2, (cutIndex >>> 10) & 31);
            display3 = copyRight(display3, cutIndex >>> 15);
        } else if (cutIndex < (1 << 25)) {
            zeroLeft(display0, cutIndex & 31);
            display1 = copyRight(display1, (cutIndex >>> 5) & 31);
            display2 = copyRight(display2, (cutIndex >>> 10) & 31);
            display3 = copyRight(display3, (cutIndex >>> 15) & 31);
            display4 = copyRight(display4, cutIndex >>> 20);
        } else if (cutIndex < (1 << 30)) {
            zeroLeft(display0, cutIndex & 31);
            display1 = copyRight(display1, (cutIndex >>> 5) & 31);
            display2 = copyRight(display2, (cutIndex >>> 10) & 31);
            display3 = copyRight(display3, (cutIndex >>> 15) & 31);
            display4 = copyRight(display4, (cutIndex >>> 20) & 31);
            display5 = copyRight(display5, cutIndex >>> 25);
        } else {
            throw new IllegalArgumentException();
        }
    }

    // requires structure is writable and at index cutIndex
    @SuppressWarnings({"DuplicatedCode"})
    private void cleanRightEdge(int cutIndex) {
        // we're actually sitting one block left if cutIndex lies on a block boundary
        // this means that we'll end up erasing the whole block!!

        if (cutIndex <= (1 << 5)) {
            zeroRight(display0, cutIndex);
        } else if (cutIndex <= (1 << 10)) {
            zeroRight(display0, ((cutIndex - 1) & 31) + 1);
            display1 = copyLeft(display1, cutIndex >>> 5);
        } else if (cutIndex <= (1 << 15)) {
            zeroRight(display0, ((cutIndex - 1) & 31) + 1);
            display1 = copyLeft(display1, (((cutIndex - 1) >>> 5) & 31) + 1);
            display2 = copyLeft(display2, cutIndex >>> 10);
        } else if (cutIndex <= (1 << 20)) {
            zeroRight(display0, ((cutIndex - 1) & 31) + 1);
            display1 = copyLeft(display1, (((cutIndex - 1) >>> 5) & 31) + 1);
            display2 = copyLeft(display2, (((cutIndex - 1) >>> 10) & 31) + 1);
            display3 = copyLeft(display3, cutIndex >>> 15);
        } else if (cutIndex <= (1 << 25)) {
            zeroRight(display0, ((cutIndex - 1) & 31) + 1);
            display1 = copyLeft(display1, (((cutIndex - 1) >>> 5) & 31) + 1);
            display2 = copyLeft(display2, (((cutIndex - 1) >>> 10) & 31) + 1);
            display3 = copyLeft(display3, (((cutIndex - 1) >>> 15) & 31) + 1);
            display4 = copyLeft(display4, cutIndex >>> 20);
        } else if (cutIndex <= (1 << 30)) {
            zeroRight(display0, ((cutIndex - 1) & 31) + 1);
            display1 = copyLeft(display1, (((cutIndex - 1) >>> 5) & 31) + 1);
            display2 = copyLeft(display2, (((cutIndex - 1) >>> 10) & 31) + 1);
            display3 = copyLeft(display3, (((cutIndex - 1) >>> 15) & 31) + 1);
            display4 = copyLeft(display4, (((cutIndex - 1) >>> 20) & 31) + 1);
            display5 = copyLeft(display5, cutIndex >>> 25);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private int requiredDepth(int xor) {
        if (xor < (1 << 5)) return 1;
        else if (xor < (1 << 10)) return 2;
        else if (xor < (1 << 15)) return 3;
        else if (xor < (1 << 20)) return 4;
        else if (xor < (1 << 25)) return 5;
        else if (xor < (1 << 30)) return 6;
        else throw new IllegalArgumentException();
    }

    @SuppressWarnings({"DuplicatedCode"})
    private Vector<A> dropFront0(int cutIndex) {
        var blockIndex = cutIndex & ~31;
        var xor = cutIndex ^ (endIndex - 1);
        var d = requiredDepth(xor);
        var shift = cutIndex & -(1 << (5 * d));

        // need to init with full display iff going to cutIndex requires swapping block at level >= d

        var s = new Vector<A>(cutIndex - shift, endIndex - shift, blockIndex - shift);
        s.initFrom(this);
        s.dirty = dirty;
        s.gotoPosWritable(focus, blockIndex, focus ^ blockIndex);
        s.preClean(d);
        s.cleanLeftEdge(cutIndex - shift);
        return s;
    }

    private Vector<A> dropBack0(int cutIndex) {
        var blockIndex = (cutIndex - 1) & ~31;
        var xor = startIndex ^ (cutIndex - 1);
        var d = requiredDepth(xor);
        var shift = startIndex & -(1 << (5 * d));

        var s = new Vector<A>(startIndex - shift, cutIndex - shift, blockIndex - shift);
        s.initFrom(this);
        s.dirty = dirty;
        s.gotoPosWritable(focus, blockIndex, focus ^ blockIndex);
        s.preClean(d);
        s.cleanRightEdge(cutIndex - shift);
        return s;
    }

    // static

    private final static int Log2ConcatFaster = 5;
    private final static int TinyAppendFaster = 2;

    private final static Vector nil = new Vector(0, 0, 0);

    @SuppressWarnings("unchecked")
    public static <A> Vector<A> empty() {
        return (Vector<A>) nil;
    }

    public static <A> VectorBuilder<A> builder() {
        return new VectorBuilder<>();
    }
}
