package com.ruthless.less.confuse;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks slow vs fast scroll to reveal delayed confused apps.
 * Uses RecyclerView scroll callbacks — no polling.
 */
public final class ScrollExposureTracker {

    public interface Listener {
        void onReveal(String packageName);
    }

    private final Map<String, ConfusePlacementEngine.Placement> placements = new HashMap<>();
    private int scrollPasses;
    private long lastScrollAt;
    private float lastDy;
    private Listener listener;

    public void beginSession(Map<String, ConfusePlacementEngine.Placement> next) {
        placements.clear();
        if (next != null) {
            placements.putAll(next);
        }
        scrollPasses = 0;
        lastScrollAt = 0;
        for (ConfusePlacementEngine.Placement p : placements.values()) {
            p.exposure = "HIDDEN";
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void onScrolled(int dy) {
        long now = System.currentTimeMillis();
        float velocityProxy = Math.abs(dy);
        boolean slow = velocityProxy < 40f;
        if (lastScrollAt > 0 && now - lastScrollAt > 400) {
            // Pause then resume counts as deliberate browsing.
            if (slow) {
                scrollPasses++;
            }
        } else if (slow && Math.abs(dy) > 0) {
            // Accumulate gentle movement toward a pass.
            if (Math.abs(lastDy) + Math.abs(dy) > 120) {
                scrollPasses++;
                lastDy = 0;
            } else {
                lastDy += dy;
            }
        } else {
            // Fast fling — do not advance delayed reveals.
            lastDy = 0;
        }
        lastScrollAt = now;

        for (ConfusePlacementEngine.Placement p : placements.values()) {
            if ("REVEALED".equals(p.exposure)) {
                continue;
            }
            if (scrollPasses >= p.targetScroll) {
                p.exposure = "ELIGIBLE";
            }
            if ("ELIGIBLE".equals(p.exposure) && slow) {
                p.exposure = "REVEALED";
                if (listener != null) {
                    listener.onReveal(p.packageName);
                }
            }
        }
    }

    public boolean isRevealed(String packageName) {
        ConfusePlacementEngine.Placement p = placements.get(packageName);
        return p == null || "REVEALED".equals(p.exposure) || p.targetScroll <= 1 && scrollPasses >= 1;
    }

    public boolean shouldHide(String packageName) {
        ConfusePlacementEngine.Placement p = placements.get(packageName);
        if (p == null) {
            return false;
        }
        return !"REVEALED".equals(p.exposure) && p.targetScroll > 1 && scrollPasses < p.targetScroll;
    }
}
