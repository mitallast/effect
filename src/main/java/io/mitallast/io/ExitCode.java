package io.mitallast.io;

public final class ExitCode {
    private final int code;

    private ExitCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static ExitCode apply(int code) {
        return new ExitCode(code);
    }

    public static final ExitCode success = new ExitCode(0);
    public static final ExitCode error = new ExitCode(1);
}
