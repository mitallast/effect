package io.mitallast.io.internals;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class ArrayStack<A> {
    public ArrayStack() {
        this(8);
    }

    public ArrayStack(int chunkSize) {
        this(new Object[chunkSize], chunkSize, 0);
    }

    private ArrayStack(Object[] initialArray, int chunkSize, int initialIndex) {
        this.chunkSize = chunkSize;
        this.array = initialArray;
        this.modulo = chunkSize - 1;
        this.index = initialIndex;
    }

    private Object[] array;
    private final int modulo;
    private int index;
    private int chunkSize;

    public boolean isEmpty() {
        return index == 0 && array[0] == null;
    }

    public void push(A a) {
        if (index == modulo) {
            var newArray = new Object[chunkSize];
            newArray[0] = array;
            array = newArray;
            index = 1;
        } else {
            index += 1;
        }
        array[index] = a;
    }

    public void pushAll(Iterator<A> cursor) {
        while (cursor.hasNext()) {
            push(cursor.next());
        }
    }

    public void pushAll(Iterable<A> cursor) {
        pushAll(cursor.iterator());
    }

    public void pushAll(ArrayStack<A> stack) {
        pushAll(stack.reverseIterator());
    }

    @SuppressWarnings("unchecked")
    public A pop() {
        if (index == 0) {
            if (array[0] != null) {
                array = (Object[]) array[0];
                index = modulo;
            } else {
                return null;
            }
        }
        A result = (A) array[index];
        array[index] = null;
        index -= 1;
        return result;
    }

    public Iterator<A> reverseIterator() {
        return new Iterator<>() {
            private Object[] array = ArrayStack.this.array;
            private int index = ArrayStack.this.index;

            @Override
            public boolean hasNext() {
                return index > 0 || array[0] != null;
            }

            @SuppressWarnings("unchecked")
            @Override
            public A next() {
                if (index == 0) {
                    array = (Object[]) array[0];
                    index = modulo;
                }
                if (array[index] == null) {
                    throw new NoSuchElementException();
                }
                A result = (A) array[index];
                index -= 1;
                return result;
            }
        };
    }
}
