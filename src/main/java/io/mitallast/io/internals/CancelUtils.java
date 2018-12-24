package io.mitallast.io.internals;

import io.mitallast.io.IO;
import io.mitallast.kernel.Unit;
import io.mitallast.list.List;

interface CancelUtils {
    static IO<Unit> cancelAll(IO... cancelables) {
        return null; // @todo implement
    }

    static IO<Unit> cancelAll(List<IO<Unit>> list) {
        return null; // @todo implement
    }
}
