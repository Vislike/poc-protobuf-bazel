package poc.server.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.concurrent.TimeUnit;

public class Utils {

    private Utils() {
    }

    public static long tickMs() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }

    private static class FormatSingleton {

        private static final FormatSingleton instance = new FormatSingleton();
        private final DecimalFormat format;

        private FormatSingleton() {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setGroupingSeparator(' ');
            format = new DecimalFormat("#,###", symbols);
        }
    }

    public static String largeNumbers(long num) {
        return FormatSingleton.instance.format.format(num);
    }
}
