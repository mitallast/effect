package io.mitallast.vector;

abstract class VectorPointer<T> {
    protected int depth;
    protected Object[] display0;
    protected Object[] display1;
    protected Object[] display2;
    protected Object[] display3;
    protected Object[] display4;
    protected Object[] display5;

    protected final <U> void initFrom(VectorPointer<U> that) {
        initFrom(that, that.depth);
    }

    protected final <U> void initFrom(VectorPointer<U> that, int depth) {
        this.depth = depth;
        if (depth >= 0) this.display0 = that.display0;
        if (depth >= 1) this.display1 = that.display1;
        if (depth >= 2) this.display2 = that.display2;
        if (depth >= 3) this.display3 = that.display3;
        if (depth >= 4) this.display4 = that.display4;
        if (depth >= 5) this.display5 = that.display5;
    }

    /**
     * requires structure is at pos oldIndex = xor ^ index
     */
    @SuppressWarnings({"unchecked", "DuplicatedCode"})
    protected final T getElem(int index, int xor) {
        if ((xor < (1 << 5))) { // level = 0
            return (T) display0[index & 31];
        } else if ((xor < (1 << 10))) { // level = 1
            var level0 = (Object[]) display1[(index >>> 5) & 31];
            return (T) level0[index & 31];
        } else if ((xor < (1 << 15))) { // level = 2
            var level1 = (Object[]) display2[(index >>> 10) & 31];
            var level0 = (Object[]) level1[(index >>> 5) & 31];
            return (T) level0[index & 31];
        } else if ((xor < (1 << 20))) { // level = 3
            var level2 = (Object[]) display3[(index >>> 15) & 31];
            var level1 = (Object[]) level2[(index >>> 10) & 31];
            var level0 = (Object[]) level1[(index >>> 5) & 31];
            return (T) level0[index & 31];
        } else if ((xor < (1 << 25))) { // level = 4
            var level3 = (Object[]) display4[(index >>> 20) & 31];
            var level2 = (Object[]) level3[(index >>> 15) & 31];
            var level1 = (Object[]) level2[(index >>> 10) & 31];
            var level0 = (Object[]) level1[(index >>> 5) & 31];
            return (T) level0[index & 31];
        } else if ((xor < (1 << 30))) { // level = 5
            var level4 = (Object[]) display5[(index >>> 25) & 31];
            var level3 = (Object[]) level4[(index >>> 20) & 31];
            var level2 = (Object[]) level3[(index >>> 15) & 31];
            var level1 = (Object[]) level2[(index >>> 10) & 31];
            var level0 = (Object[]) level1[(index >>> 5) & 31];
            return (T) level0[index & 31];
        } else throw new IllegalArgumentException(); // level = 6
    }

    /**
     * go to specific position
     * requires structure is at pos oldIndex = xor ^ index,
     * ensures structure is at pos index
     */
    protected final void gotoPos(int index, int xor) {
        if ((xor < (1 << 5))) { // level = 0
            // we're already at the block start pos
        } else if ((xor < (1 << 10))) { // level = 1
            display0 = (Object[]) display1[(index >>> 5) & 31];
        } else if ((xor < (1 << 15))) { // level = 2
            display1 = (Object[]) display2[(index >>> 10) & 31];
            display0 = (Object[]) display1[(index >>> 5) & 31];
        } else if ((xor < (1 << 20))) { // level = 3
            display2 = (Object[]) display3[(index >>> 15) & 31];
            display1 = (Object[]) display2[(index >>> 10) & 31];
            display0 = (Object[]) display1[(index >>> 5) & 31];
        } else if ((xor < (1 << 25))) { // level = 4
            display3 = (Object[]) display4[(index >>> 20) & 31];
            display2 = (Object[]) display3[(index >>> 15) & 31];
            display1 = (Object[]) display2[(index >>> 10) & 31];
            display0 = (Object[]) display1[(index >>> 5) & 31];
        } else if ((xor < (1 << 30))) { // level = 5
            display4 = (Object[]) display5[(index >>> 25) & 31];
            display3 = (Object[]) display4[(index >>> 20) & 31];
            display2 = (Object[]) display3[(index >>> 15) & 31];
            display1 = (Object[]) display2[(index >>> 10) & 31];
            display0 = (Object[]) display1[(index >>> 5) & 31];
        } else throw new IllegalArgumentException(); // level = 6
    }

    // USED BY ITERATOR

    // xor: oldIndex ^ index
    protected final void gotoNextBlockStart(int index, int xor) {
        if ((xor < (1 << 10))) { // level = 1
            display0 = (Object[]) display1[(index >>> 5) & 31];
        } else if ((xor < (1 << 15))) { // level = 2
            display1 = (Object[]) display2[(index >>> 10) & 31];
            display0 = (Object[]) display1[0];
        } else if ((xor < (1 << 20))) { // level = 3
            display2 = (Object[]) display3[(index >>> 15) & 31];
            display1 = (Object[]) display2[0];
            display0 = (Object[]) display1[0];
        } else if ((xor < (1 << 25))) { // level = 4
            display3 = (Object[]) display4[(index >>> 20) & 31];
            display2 = (Object[]) display3[0];
            display1 = (Object[]) display2[0];
            display0 = (Object[]) display1[0];
        } else if ((xor < (1 << 30))) { // level = 5
            display4 = (Object[]) display5[(index >>> 25) & 31];
            display3 = (Object[]) display4[0];
            display2 = (Object[]) display3[0];
            display1 = (Object[]) display2[0];
            display0 = (Object[]) display1[0];
        } else throw new IllegalArgumentException(); // level = 6
    }

    // USED BY BUILDER

    // xor: oldIndex ^ index
    @SuppressWarnings({"DuplicatedCode"})
    protected final void gotoNextBlockStartWritable(int index, int xor) {
        // goto block start pos
        if ((xor < (1 << 10))) { // level = 1
            if (depth == 1) {
                display1 = new Object[32];
                display1[0] = display0;
                depth++;
            }
            display0 = new Object[32];
            display1[(index >>> 5) & 31] = display0;
        } else if ((xor < (1 << 15))) { // level = 2
            if (depth == 2) {
                display2 = new Object[32];
                display2[0] = display1;
                depth++;
            }
            display0 = new Object[32];
            display1 = new Object[32];
            display1[(index >>> 5) & 31] = display0;
            display2[(index >>> 10) & 31] = display1;
        } else if ((xor < (1 << 20))) { // level = 3
            if (depth == 3) {
                display3 = new Object[32];
                display3[0] = display2;
                depth++;
            }
            display0 = new Object[32];
            display1 = new Object[32];
            display2 = new Object[32];
            display1[(index >>> 5) & 31] = display0;
            display2[(index >>> 10) & 31] = display1;
            display3[(index >>> 15) & 31] = display2;
        } else if ((xor < (1 << 25))) { // level = 4
            if (depth == 4) {
                display4 = new Object[32];
                display4[0] = display3;
                depth++;
            }
            display0 = new Object[32];
            display1 = new Object[32];
            display2 = new Object[32];
            display3 = new Object[32];
            display1[(index >>> 5) & 31] = display0;
            display2[(index >>> 10) & 31] = display1;
            display3[(index >>> 15) & 31] = display2;
            display4[(index >>> 20) & 31] = display3;
        } else if ((xor < (1 << 30))) { // level = 5
            if (depth == 5) {
                display5 = new Object[32];
                display5[0] = display4;
                depth++;
            }
            display0 = new Object[32];
            display1 = new Object[32];
            display2 = new Object[32];
            display3 = new Object[32];
            display4 = new Object[32];
            display1[(index >>> 5) & 31] = display0;
            display2[(index >>> 10) & 31] = display1;
            display3[(index >>> 15) & 31] = display2;
            display4[(index >>> 20) & 31] = display3;
            display5[(index >>> 25) & 31] = display4;
        } else throw new IllegalArgumentException(); // level = 6
    }

    // STUFF BELOW USED BY APPEND / UPDATE

    protected final Object[] copyOf(Object[] a) {
        var copy = new Object[a.length];
        System.arraycopy(a, 0, copy, 0, a.length);
        return copy;
    }

    protected final Object[] nullSlotAndCopy(Object[] array, int index) {
        var x = array[index];
        array[index] = null;
        return copyOf((Object[]) x);
    }

    // make sure there is no aliasing
    // requires structure is at pos index
    // ensures structure is clean and at pos index and writable at all levels except 0
    protected final void stabilize(int index) {
        switch (depth - 1) {
            case 5:
                display5 = copyOf(display5);
                display4 = copyOf(display4);
                display3 = copyOf(display3);
                display2 = copyOf(display2);
                display1 = copyOf(display1);
                display5[(index >>> 25) & 31] = display4;
                display4[(index >>> 20) & 31] = display3;
                display3[(index >>> 15) & 31] = display2;
                display2[(index >>> 10) & 31] = display1;
                display1[(index >>> 5) & 31] = display0;
                break;
            case 4:
                display4 = copyOf(display4);
                display3 = copyOf(display3);
                display2 = copyOf(display2);
                display1 = copyOf(display1);
                display4[(index >>> 20) & 31] = display3;
                display3[(index >>> 15) & 31] = display2;
                display2[(index >>> 10) & 31] = display1;
                display1[(index >>> 5) & 31] = display0;
                break;
            case 3:
                display3 = copyOf(display3);
                display2 = copyOf(display2);
                display1 = copyOf(display1);
                display3[(index >>> 15) & 31] = display2;
                display2[(index >>> 10) & 31] = display1;
                display1[(index >>> 5) & 31] = display0;
                break;
            case 2:
                display2 = copyOf(display2);
                display1 = copyOf(display1);
                display2[(index >>> 10) & 31] = display1;
                display1[(index >>> 5) & 31] = display0;
                break;
            case 1:
                display1 = copyOf(display1);
                display1[(index >>> 5) & 31] = display0;
                break;
        }
    }

    /// USED IN UPDATE AND APPEND BACK

    // prepare for writing at an existing position

    // requires structure is clean and at pos oldIndex = xor ^ newIndex,
    // ensures structure is dirty and at pos newIndex and writable at level 0
    protected final void gotoPosWritable0(int newIndex, int xor) {
        switch (depth - 1) {
            case 5:
                display5 = copyOf(display5);
                display4 = nullSlotAndCopy(display5, (newIndex >>> 25) & 31);
                display3 = nullSlotAndCopy(display4, (newIndex >>> 20) & 31);
                display2 = nullSlotAndCopy(display3, (newIndex >>> 15) & 31);
                display1 = nullSlotAndCopy(display2, (newIndex >>> 10) & 31);
                display0 = nullSlotAndCopy(display1, (newIndex >>> 5) & 31);
                break;
            case 4:
                display4 = copyOf(display4);
                display3 = nullSlotAndCopy(display4, (newIndex >>> 20) & 31);
                display2 = nullSlotAndCopy(display3, (newIndex >>> 15) & 31);
                display1 = nullSlotAndCopy(display2, (newIndex >>> 10) & 31);
                display0 = nullSlotAndCopy(display1, (newIndex >>> 5) & 31);
                break;
            case 3:
                display3 = copyOf(display3);
                display2 = nullSlotAndCopy(display3, (newIndex >>> 15) & 31);
                display1 = nullSlotAndCopy(display2, (newIndex >>> 10) & 31);
                display0 = nullSlotAndCopy(display1, (newIndex >>> 5) & 31);
                break;
            case 2:
                display2 = copyOf(display2);
                display1 = nullSlotAndCopy(display2, (newIndex >>> 10) & 31);
                display0 = nullSlotAndCopy(display1, (newIndex >>> 5) & 31);
                break;
            case 1:
                display1 = copyOf(display1);
                display0 = nullSlotAndCopy(display1, (newIndex >>> 5) & 31);
                break;
            case 0:
                display0 = copyOf(display0);
                break;
        }
    }

    // requires structure is dirty and at pos oldIndex,
    // ensures structure is dirty and at pos newIndex and writable at level 0
    @SuppressWarnings({"DuplicatedCode"})
    protected final void gotoPosWritable1(int oldIndex, int newIndex, int xor) {
        if ((xor < (1 << 5))) { // level = 0
            display0 = copyOf(display0);
        } else if ((xor < (1 << 10))) { // level = 1
            display1 = copyOf(display1);
            display1[(oldIndex >>> 5) & 31] = display0;
            display0 = nullSlotAndCopy(display1, (newIndex >>> 5) & 31);
        } else if ((xor < (1 << 15))) { // level = 2
            display1 = copyOf(display1);
            display2 = copyOf(display2);
            display1[(oldIndex >>> 5) & 31] = display0;
            display2[(oldIndex >>> 10) & 31] = display1;
            display1 = nullSlotAndCopy(display2, (newIndex >>> 10) & 31);
            display0 = nullSlotAndCopy(display1, (newIndex >>> 5) & 31);
        } else if ((xor < (1 << 20))) { // level = 3
            display1 = copyOf(display1);
            display2 = copyOf(display2);
            display3 = copyOf(display3);
            display1[(oldIndex >>> 5) & 31] = display0;
            display2[(oldIndex >>> 10) & 31] = display1;
            display3[(oldIndex >>> 15) & 31] = display2;
            display2 = nullSlotAndCopy(display3, (newIndex >>> 15) & 31);
            display1 = nullSlotAndCopy(display2, (newIndex >>> 10) & 31);
            display0 = nullSlotAndCopy(display1, (newIndex >>> 5) & 31);
        } else if ((xor < (1 << 25))) { // level = 4
            display1 = copyOf(display1);
            display2 = copyOf(display2);
            display3 = copyOf(display3);
            display4 = copyOf(display4);
            display1[(oldIndex >>> 5) & 31] = display0;
            display2[(oldIndex >>> 10) & 31] = display1;
            display3[(oldIndex >>> 15) & 31] = display2;
            display4[(oldIndex >>> 20) & 31] = display3;
            display3 = nullSlotAndCopy(display4, (newIndex >>> 20) & 31);
            display2 = nullSlotAndCopy(display3, (newIndex >>> 15) & 31);
            display1 = nullSlotAndCopy(display2, (newIndex >>> 10) & 31);
            display0 = nullSlotAndCopy(display1, (newIndex >>> 5) & 31);
        } else if ((xor < (1 << 30))) { // level = 5
            display1 = copyOf(display1);
            display2 = copyOf(display2);
            display3 = copyOf(display3);
            display4 = copyOf(display4);
            display5 = copyOf(display5);
            display1[(oldIndex >>> 5) & 31] = display0;
            display2[(oldIndex >>> 10) & 31] = display1;
            display3[(oldIndex >>> 15) & 31] = display2;
            display4[(oldIndex >>> 20) & 31] = display3;
            display5[(oldIndex >>> 25) & 31] = display5;
            display4 = nullSlotAndCopy(display5, (newIndex >>> 25) & 31);
            display3 = nullSlotAndCopy(display4, (newIndex >>> 20) & 31);
            display2 = nullSlotAndCopy(display3, (newIndex >>> 15) & 31);
            display1 = nullSlotAndCopy(display2, (newIndex >>> 10) & 31);
            display0 = nullSlotAndCopy(display1, (newIndex >>> 5) & 31);
        } else throw new IllegalArgumentException(); // level = 6
    }

    // USED IN DROP

    protected final Object[] copyRange(Object[] array, int oldLeft, int newLeft) {
        var elems = new Object[32];
        System.arraycopy(array, oldLeft, elems, newLeft, 32 - Math.max(newLeft, oldLeft));
        return elems;
    }

    // USED IN APPEND
    // create a new block at the bottom level (and possibly nodes on its path) and prepares for writing

    // requires structure is clean and at pos oldIndex,
    // ensures structure is dirty and at pos newIndex and writable at level 0
    @SuppressWarnings({"DuplicatedCode"})
    protected final void gotoFreshPosWritable0(int oldIndex, int newIndex, int xor) {
        // goto block start pos
        if ((xor < (1 << 5))) { // level = 0
            // we're already at the block start
        } else if ((xor < (1 << 10))) { // level = 1
            if (depth == 1) {
                display1 = new Object[32];
                display1[(oldIndex >>> 5) & 31] = display0;
                depth++;
            }
            display0 = new Object[32];
        } else if ((xor < (1 << 15))) { // level = 2
            if (depth == 2) {
                display2 = new Object[32];
                display2[(oldIndex >>> 10) & 31] = display1;
                depth++;
            }
            display1 = (Object[]) display2[(newIndex >>> 10) & 31];
            if (display1 == null) display1 = new Object[32];
            display0 = new Object[32];
        } else if ((xor < (1 << 20))) { // level = 3
            if (depth == 3) {
                display3 = new Object[32];
                display3[(oldIndex >>> 15) & 31] = display2;
                depth++;
            }
            display2 = (Object[]) display3[(newIndex >>> 15) & 31];
            if (display2 == null) display2 = new Object[32];
            display1 = (Object[]) display2[(newIndex >>> 10) & 31];
            if (display1 == null) display1 = new Object[32];
            display0 = new Object[32];
        } else if ((xor < (1 << 25))) { // level = 4
            if (depth == 4) {
                display4 = new Object[32];
                display4[(oldIndex >>> 20) & 31] = display3;
                depth++;
            }
            display3 = (Object[]) display4[(newIndex >>> 20) & 31];
            if (display3 == null) display3 = new Object[32];
            display2 = (Object[]) display3[(newIndex >>> 15) & 31];
            if (display2 == null) display2 = new Object[32];
            display1 = (Object[]) display2[(newIndex >>> 10) & 31];
            if (display1 == null) display1 = new Object[32];
            display0 = new Object[32];
        } else if ((xor < (1 << 30))) { // level = 5
            if (depth == 5) {
                display5 = new Object[32];
                display5[(oldIndex >>> 25) & 31] = display4;
                depth++;
            }
            display4 = (Object[]) display5[(newIndex >>> 25) & 31];
            if (display4 == null) display4 = new Object[32];
            display3 = (Object[]) display4[(newIndex >>> 20) & 31];
            if (display3 == null) display3 = new Object[32];
            display2 = (Object[]) display3[(newIndex >>> 15) & 31];
            if (display2 == null) display2 = new Object[32];
            display1 = (Object[]) display2[(newIndex >>> 10) & 31];
            if (display1 == null) display1 = new Object[32];
            display0 = new Object[32];
        } else throw new IllegalArgumentException(); // level = 6
    }

    // requires structure is dirty and at pos oldIndex,
    // ensures structure is dirty and at pos newIndex and writable at level 0
    protected final void gotoFreshPosWritable1(int oldIndex, int newIndex, int xor) {
        stabilize(oldIndex);
        gotoFreshPosWritable0(oldIndex, newIndex, xor);
    }
}
