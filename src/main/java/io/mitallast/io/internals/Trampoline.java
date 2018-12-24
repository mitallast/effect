package io.mitallast.io.internals;

import io.mitallast.concurrent.ExecutionContext;

class Trampoline {
    private final ExecutionContext underlying;

    private ArrayStack<Runnable> immediateQueue = new ArrayStack<>();
    private boolean withinLoop = false;

    Trampoline(ExecutionContext underlying) {
        this.underlying = underlying;
    }

    public void startLoop(Runnable runnable) {
        withinLoop = true;
        try {
            immediateLoop(runnable);
        } finally {
            withinLoop = false;
        }
    }

    public void execute(Runnable runnable) {
        if (!withinLoop) {
            startLoop(runnable);
        } else {
            immediateQueue.push(runnable);
        }
    }

    protected void forkTheRest() {
        var head = immediateQueue.pop();
        if (head != null) {
            var rest = immediateQueue;
            immediateQueue = new ArrayStack<>();
            underlying.execute(new ResumeRun(head, rest));
        }
    }

    private void immediateLoop(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            forkTheRest();
            underlying.reportFailure(e);
        }
    }

    private class ResumeRun implements Runnable {
        private final Runnable head;
        private final ArrayStack<Runnable> rest;

        private ResumeRun(Runnable head, ArrayStack<Runnable> rest) {
            this.head = head;
            this.rest = rest;
        }

        @Override
        public void run() {
            immediateQueue.pushAll(rest);
            immediateLoop(head);
        }
    }
}
