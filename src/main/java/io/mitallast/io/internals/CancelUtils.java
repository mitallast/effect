package io.mitallast.io.internals;

import io.mitallast.io.IO;
import io.mitallast.kernel.Unit;

interface CancelUtils {
    static IO<Unit> cancelAll(IO... cancelables) {
        return null;
    }
}
