package com.ruthless.less.usage;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure logic for overall (not per-app) screen-time milestone alerts.
 * Steps: 10, 20, 30, … minutes.
 */
public final class ScreenTimeAlertLogic {

    public static final int STEP_MINUTES = 10;
    /** Safety cap so a stuck timer cannot spam forever. */
    public static final int MAX_MILESTONE_MINUTES = 12 * 60;

    private static final String[] LINES = {
            "OVERALL SCREEN TIME",
            "STILL ON THE SCREEN",
            "THE SCREEN IS STILL ON",
            "LONG SESSION",
            "YOU ARE STILL LOOKING",
            "TIME IS PASSING",
            "PUT IT DOWN SOON",
            "THIS IS A LONG STRETCH"
    };

    private ScreenTimeAlertLogic() {
    }

    /**
     * @param totalMinutes overall minutes so far (today or continuous session)
     * @param lastFiredMilestone last notified step (0 if none); must be multiple of STEP or 0
     * @return milestones to notify now, ascending (e.g. 10, 20)
     */
    public static List<Integer> pendingMilestones(int totalMinutes, int lastFiredMilestone) {
        List<Integer> out = new ArrayList<>();
        if (totalMinutes < STEP_MINUTES) {
            return out;
        }
        int top = Math.min((totalMinutes / STEP_MINUTES) * STEP_MINUTES, MAX_MILESTONE_MINUTES);
        int start = Math.max(STEP_MINUTES, lastFiredMilestone + STEP_MINUTES);
        // Align start to step grid if lastFired was weird.
        if (start % STEP_MINUTES != 0) {
            start = ((start / STEP_MINUTES) + 1) * STEP_MINUTES;
        }
        for (int m = start; m <= top; m += STEP_MINUTES) {
            out.add(m);
        }
        return out;
    }

    public static String titleFor(int milestoneMinutes) {
        return LINES[(Math.max(0, milestoneMinutes) / STEP_MINUTES) % LINES.length];
    }

    public static String bodyFor(int milestoneMinutes, boolean dailyTotal) {
        if (dailyTotal) {
            return milestoneMinutes + " MINUTES TODAY";
        }
        return milestoneMinutes + " MINUTES SCREEN ON";
    }
}
