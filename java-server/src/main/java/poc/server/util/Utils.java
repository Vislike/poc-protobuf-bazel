package poc.server.util;

import java.util.concurrent.TimeUnit;

public class Utils {
    private Utils() {
    }

    public static long tickMs() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }
}
