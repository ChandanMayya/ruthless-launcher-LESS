package com.ruthless.less.friction;

import java.util.Random;

/**
 * Random launch delay — never a fixed wait.
 */
public final class LaunchDelayManager {

    private final Random random = new Random();

    /**
     * @return delay in milliseconds within [minMs, maxMs], or 0 if mode is OFF / invalid.
     */
    public long nextDelayMs(String mode, int minMs, int maxMs) {
        if (mode == null || "OFF".equals(mode)) {
            return 0L;
        }
        int min = Math.max(0, minMs);
        int max = Math.max(min, maxMs);
        if (max <= 0) {
            return 0L;
        }
        if (max == min) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }
}
