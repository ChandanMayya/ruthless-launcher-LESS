package com.ruthless.less.usage;

/**
 * Behavioral phone-temperature metric from today's usage vs personal average.
 * Not medical or scientific.
 */
public final class PhoneTemperature {

    public enum Level {
        COLD, COOL, NORMAL, WARM, HOT, ON_FIRE
    }

    public static Level fromMinutes(int todayMinutes, int averageMinutes) {
        if (averageMinutes <= 0) {
            // No history yet — use absolute buckets.
            if (todayMinutes < 30) return Level.COLD;
            if (todayMinutes < 60) return Level.COOL;
            if (todayMinutes < 120) return Level.NORMAL;
            if (todayMinutes < 180) return Level.WARM;
            if (todayMinutes < 300) return Level.HOT;
            return Level.ON_FIRE;
        }
        double ratio = (double) todayMinutes / (double) averageMinutes;
        if (ratio < 0.5) return Level.COLD;
        if (ratio < 0.8) return Level.COOL;
        if (ratio < 1.1) return Level.NORMAL;
        if (ratio < 1.4) return Level.WARM;
        if (ratio < 1.8) return Level.HOT;
        return Level.ON_FIRE;
    }

    public static String label(Level level) {
        return level.name().replace('_', ' ');
    }
}
