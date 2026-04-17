package app.common;

/** Utility helpers for formatting timestamps used by analysis and compliance outputs. */
public class TimestampUtils {

    private TimestampUtils() {}

    public static String formatTimestamp(double seconds) {
        int h = (int) (seconds / 3600);
        int m = (int) ((seconds % 3600) / 60);
        int s = (int) (seconds % 60);
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}