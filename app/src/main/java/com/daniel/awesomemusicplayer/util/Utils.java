package com.daniel.awesomemusicplayer.util;

/**
 * Utilities class - contains static utility methods
 */
public class Utils {

    public static String formatSeconds(long seconds) {
        return formatMillis(seconds * 1000L);
    }

    public static String formatMillis(long millis) {
        StringBuilder sb = new StringBuilder();
        String sign = "";

        // Add sign if needed
        if (millis < 0) {
            sign = "-";
            millis *= -1;
        }

        // Calculate and format
//        apd(sb, sign, 0, (millis / 3600000));
        millis %= 3600000;
        apd(sb, "", 2, (millis / 60000));
        millis %= 60000;
        apd(sb, ":", 2, (millis / 1000));
//        millis %= 1000;
//        apd(sb, ".", 3, millis);

        return sb.toString();
    }

    private static void apd(StringBuilder sb, String prefix, int digit, long val) {
        sb.append(prefix);

        if (digit > 1) {
            int pad = digit - 1;
            for (long xa = val; xa > 9 && pad > 0; xa /= 10)
                pad--;
            for (int xa = 0; xa < pad; xa++)
                sb.append('0');
        }
        sb.append(val);
    }


}

