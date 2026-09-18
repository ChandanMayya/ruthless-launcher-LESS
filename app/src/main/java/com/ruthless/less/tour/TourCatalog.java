package com.ruthless.less.tour;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Narrative catalog: first-run “Future You” tour + revisitable Field Manual.
 */
public final class TourCatalog {

    public static final String CHAPTER_HOME = "home";
    public static final String CHAPTER_EVIDENCE = "evidence";
    public static final String CHAPTER_PAUSE = "pause";
    public static final String CHAPTER_SESSION = "session";
    public static final String CHAPTER_LIMITS = "limits";
    public static final String CHAPTER_CONFUSE = "confuse";
    public static final String CHAPTER_GATES = "gates";
    public static final String CHAPTER_CONTROL = "control";

    private TourCatalog() {
    }

    /** First-run beats in order. Boundary step is interactive. */
    public static List<TourBeat> firstRunBeats() {
        return Collections.unmodifiableList(Arrays.asList(
                TourBeat.builder("fr_open")
                        .section("A NOTE FROM TOMORROW")
                        .title("LESS")
                        .lead("Tomorrow, you may not remember why you opened this.")
                        .body(
                                "LESS is a home screen that stays useful and refuses to entertain you.",
                                "Black and white text. No icons. No feeds. Offline. Private.")
                        .warning("LESS does not fix habits. It only makes the next open a little harder to do on impulse.")
                        .primary("CONTINUE", TourAction.NEXT)
                        .demoSlot("fr_open")
                        .build(),

                TourBeat.builder("fr_trap")
                        .section("A · THE FIRST TRAP")
                        .title("HOME SHOULD NOT ENTERTAIN YOU")
                        .lead("A useful home reduces reflexive opening. It does not offer more temptation.")
                        .body(
                                "LESS keeps a sparse home: a few intentional apps only.",
                                "Everything else waits in the drawer. Search finds names — not shiny bait.",
                                "Swipe up for applications. Long-press the clock for Settings later.")
                        .warning("Until LESS is your default launcher, the old home still appears first.")
                        .primary("OPEN LAUNCHER SETTINGS", TourAction.OPEN_HOME_SETTINGS)
                        .secondary("NEXT", TourAction.NEXT)
                        .check(TourCheck.DEFAULT_HOME)
                        .demoSlot("fr_home")
                        .build(),

                TourBeat.builder("fr_evidence")
                        .section("B · THE EVIDENCE")
                        .title("YOUR PHONE KEEPS RECEIPTS")
                        .lead("The phone remembers the returns.")
                        .body(
                                "Screen time, pickups, last unlock, and phone temperature are evidence of repeated returns — not entertainment.",
                                "Phone temperature compares today to your own recent average. It is not medical advice, battery health, or a hardware diagnosis.",
                                "Usage Access is needed to read these receipts. Data stays on this device. Nothing is uploaded.")
                        .warning("Without Usage Access, totals and temperature stay blank. That is optional, and reversible in system settings.")
                        .primary("OPEN USAGE ACCESS", TourAction.OPEN_USAGE_ACCESS)
                        .secondary("NEXT", TourAction.NEXT)
                        .check(TourCheck.USAGE_ACCESS)
                        .demoSlot("fr_evidence")
                        .build(),

                TourBeat.builder("fr_accessibility")
                        .section("B · THE HELPERS")
                        .title("ACCESSIBILITY")
                        .lead("Optional helpers that act when a limit ends, or when you swipe and double-tap home.")
                        .body(
                                "What it enables: return home when a daily quota is finished; double-tap empty home to lock; swipe down for notifications or quick settings.",
                                "How to turn it on:",
                                "1. Tap OPEN ACCESSIBILITY below.",
                                "2. Find LESS in the list and open it.",
                                "3. Turn the main LESS switch On.",
                                "4. Leave LESS shortcut Off — that floating button is not used.")
                        .warning("Optional and reversible. Not a hard jail. You can skip and enable later from Settings.")
                        .primary("OPEN ACCESSIBILITY", TourAction.OPEN_ACCESSIBILITY)
                        .secondary("NEXT", TourAction.NEXT)
                        .check(TourCheck.ACCESSIBILITY)
                        .demoSlot("fr_a11y")
                        .build(),

                TourBeat.builder("fr_overlay")
                        .section("B · THE HELPERS")
                        .title("DISPLAY OVER APPS")
                        .lead("Session timers need a small countdown on top of other apps.")
                        .body(
                                "When a session timer is on, LESS asks how long before the app opens, then shows a text countdown.",
                                "How to allow it:",
                                "1. Tap OPEN DISPLAY OVER APPS below.",
                                "2. Find LESS and allow display over other apps.")
                        .warning("Optional. Only needed for session timers. Reversible in system settings.")
                        .primary("OPEN DISPLAY OVER APPS", TourAction.OPEN_OVERLAY_SETTINGS)
                        .secondary("NEXT", TourAction.NEXT)
                        .check(TourCheck.OVERLAY)
                        .demoSlot("fr_overlay")
                        .build(),

                TourBeat.builder("fr_notifications")
                        .section("B · THE HELPERS")
                        .title("NOTIFICATIONS")
                        .lead("Overall screen-time alerts need permission to notify you.")
                        .body(
                                "LESS can remind you at 10 / 20 / 30 minutes of overall screen time.",
                                "Tap ALLOW NOTIFICATIONS to grant it. You can deny and enable later.")
                        .warning("Optional. Screen-time alerts work only if notifications are allowed.")
                        .primary("ALLOW NOTIFICATIONS", TourAction.REQUEST_NOTIFICATIONS)
                        .secondary("NEXT", TourAction.NEXT)
                        .check(TourCheck.NOTIFICATIONS)
                        .demoSlot("fr_notif")
                        .build(),

                TourBeat.builder("fr_pause")
                        .section("C · THE PAUSE")
                        .title("MAKE AN IMPULSE ANSWER A QUESTION")
                        .lead("A reflex is fast. Intention is usually not.")
                        .body(
                                "Launch confirmation asks OPEN? before the app starts.",
                                "A short launch delay (for example 0–10 seconds) inserts waiting between tap and open.",
                                "Either one is a small boundary you set while you are calm.")
                        .warning("These are soft pauses — not a hard jail. You can remove them anytime.")
                        .primary("SET ONE BOUNDARY", TourAction.NEXT)
                        .demoSlot("fr_pause")
                        .build(),

                TourBeat.builder("fr_boundary")
                        .section("C · THE PAUSE")
                        .title("SET ONE BOUNDARY WHILE YOU ARE CALM")
                        .lead("Choose one app. Add confirmation, or a short delay. Or begin with none.")
                        .body(
                                "Pick an app you open without thinking.",
                                "Confirmation asks before it opens. Delay waits briefly first.")
                        .warning("Skipping is allowed. No boundary yet is still a complete setup.")
                        .boundaryStep()
                        .demoSlot("fr_boundary")
                        .build(),

                TourBeat.builder("fr_ready")
                        .section("A NOTE FROM TOMORROW")
                        .title("BEGIN")
                        .lead("Setup is done. The Field Manual is there if you want the full map later.")
                        .body(
                                "Long-press any app for limits, CONFUSE ME, session timers, and more.",
                                "Long-press the clock for Settings.")
                        .warning("Put the phone down when you can.")
                        .primary("BEGIN", TourAction.FINISH_ONBOARDING)
                        .build()
        ));
    }

    /** Index entries for Field Manual (one row per chapter). */
    public static List<TourBeat> fieldManualIndex() {
        List<TourBeat> chapters = fieldManualChapters();
        List<TourBeat> index = new ArrayList<>(chapters.size());
        for (TourBeat chapter : chapters) {
            index.add(TourBeat.builder("idx_" + chapter.id)
                    .section("FIELD MANUAL")
                    .title(chapter.title)
                    .lead(chapter.lead)
                    .body()
                    .primary("OPEN", TourAction.OPEN_FIELD_CHAPTER)
                    .chapterId(chapter.id)
                    .build());
        }
        return Collections.unmodifiableList(index);
    }

    public static List<TourBeat> fieldManualChapters() {
        return Collections.unmodifiableList(Arrays.asList(
                TourBeat.builder(CHAPTER_HOME)
                        .section("FIELD MANUAL · 1")
                        .title("HOME, DRAWER, SEARCH, PINS")
                        .lead("Tomorrow you may open the phone with no errand — only habit.")
                        .body(
                                "Home shows only apps you place there. The drawer holds the rest. Search matches text names.",
                                "Pins float to the top of the drawer. Long-press an app to pin, unpin, or change home membership.")
                        .warning("A crowded home recreates the old trap. Keep home intentional.")
                        .primary("HOME APPS", TourAction.OPEN_HOME_APPS)
                        .secondary("PINNED APPS", TourAction.OPEN_PINNED_APPS)
                        .demoSlot("fm_home")
                        .build(),

                TourBeat.builder(CHAPTER_EVIDENCE)
                        .section("FIELD MANUAL · 2")
                        .title("RECEIPTS: TIME, PICKUPS, TEMPERATURE")
                        .lead("The phone remembers the returns.")
                        .body(
                                "Screen time totals and per-app minutes come from Android usage stats.",
                                "Pickups approximate unlock or interactive sessions. Last unlock is when you last entered LESS home.",
                                "Phone temperature compares today to your recent average — personal signal only.",
                                "Screen-time alerts can notify at overall 10 / 20 / 30… minute milestones while the screen stays on.")
                        .warning("Not medical. Not battery health. OEM reporting varies. Usage Access is optional and on-device only.")
                        .primary("SCREEN TIME", TourAction.OPEN_SCREEN_TIME)
                        .secondary("USAGE ACCESS", TourAction.OPEN_USAGE_ACCESS)
                        .demoSlot("fm_evidence")
                        .build(),

                TourBeat.builder(CHAPTER_PAUSE)
                        .section("FIELD MANUAL · 3")
                        .title("CONFIRMATION AND DELAY")
                        .lead("A reflex is fast. Intention is usually not.")
                        .body(
                                "Launch confirmation inserts OPEN? before the app starts.",
                                "Launch delay waits a chosen range (including a short 0–10 second pause) before opening.")
                        .warning("Soft friction only. You can disable either anytime from the app long-press menu.")
                        .primary("LAUNCH CONFIRMATION", TourAction.OPEN_POLICY_CONFIRMATION)
                        .secondary("LAUNCH DELAYS", TourAction.OPEN_POLICY_DELAYS)
                        .demoSlot("fm_pause")
                        .build(),

                TourBeat.builder(CHAPTER_SESSION)
                        .section("FIELD MANUAL · 4")
                        .title("SESSION TIMERS")
                        .lead("Decide the endpoint before the session begins.")
                        .body(
                                "When enabled, LESS asks how long (1 / 5 / 10 / 15 minutes) before the app opens.",
                                "A countdown runs while that app stays in front. Leave the app (or return home) and the timer stops.",
                                "If time runs out while you are still in the app, LESS returns you home. It cannot force-stop the other app.")
                        .warning("Needs Display over other apps for the overlay. Leaving detection is stronger with Accessibility On. Optional. Reversible.")
                        .primary("SESSION TIMERS", TourAction.OPEN_POLICY_SESSION_TIMER)
                        .secondary("DISPLAY OVER APPS", TourAction.OPEN_OVERLAY_SETTINGS)
                        .demoSlot("fm_session")
                        .build(),

                TourBeat.builder(CHAPTER_LIMITS)
                        .section("FIELD MANUAL · 5")
                        .title("DAILY LIMITS")
                        .lead("One day of attention is finite. Tomorrow resets the clock.")
                        .body(
                                "A daily limit caps minutes in one app. When finished, LESS blocks further opens that day.",
                                "You get one intentional five-minute extension per app per day. After that, wait until tomorrow.",
                                "Accessibility can return you home if a limited app reaches the foreground after the quota ends.")
                        .warning("Not a hard OS jail. Revoking Accessibility or changing defaults weakens enforcement.")
                        .primary("APP LIMITS", TourAction.OPEN_POLICY_LIMITS)
                        .secondary("ACCESSIBILITY", TourAction.OPEN_ACCESSIBILITY)
                        .demoSlot("fm_limits")
                        .build(),

                TourBeat.builder(CHAPTER_CONFUSE)
                        .section("FIELD MANUAL · 6")
                        .title("CONFUSE ME")
                        .lead("Easy to find becomes easy to open.")
                        .body(
                                "CONFUSE ME removes the app from Home and pins, and hides it from search.",
                                "Opening still asks for confirmation. Declining reshuffles where that name sits in the drawer.",
                                "You can turn it off later. The app remains installed.")
                        .warning("Friction, not deletion. Reversible from the same long-press menu.")
                        .primary("CONFUSE ME LIST", TourAction.OPEN_POLICY_CONFUSE)
                        .demoSlot("fm_confuse")
                        .build(),

                TourBeat.builder(CHAPTER_GATES)
                        .section("FIELD MANUAL · 7")
                        .title("BOREDOM AND GESTURES")
                        .lead("A quiet home can interrupt the rush without replacing your system lock.")
                        .body(
                                "Boredom Mode shows chiding text on home and in the drawer.",
                                "Home gestures: swipe up for apps; swipe down on the left for the notification shade, on the right for quick settings; double-tap empty home to lock the device (Accessibility).",
                                "These helpers are offered during first-run setup. You can change them anytime in Settings.")
                        .warning("Turn the main LESS Accessibility switch On. Leave LESS shortcut Off — the floating button is not used. All optional and reversible.")
                        .primary("ACCESSIBILITY", TourAction.OPEN_ACCESSIBILITY)
                        .demoSlot("fm_gates")
                        .build(),

                TourBeat.builder(CHAPTER_CONTROL)
                        .section("FIELD MANUAL · 8")
                        .title("POLICIES, DATA, PRIVACY")
                        .lead("You stay in control of every boundary.")
                        .body(
                                "Settings lists policy filters: limits, delays, confirmation, session timers, exclusions, CONFUSE ME.",
                                "Exclusions omit an app from screen-time totals used for temperature and quotas when respected.",
                                "DATA clears local history and friction state. There is no account and no cloud.",
                                "Permissions stay optional: Usage Access, Accessibility, Display over apps, default launcher.")
                        .warning("Clearing data cannot recover Android’s own discarded usage history.")
                        .primary("DATA", TourAction.OPEN_DATA)
                        .secondary("EXCLUSIONS", TourAction.OPEN_POLICY_EXCLUSIONS)
                        .demoSlot("fm_control")
                        .build()
        ));
    }

    public static TourBeat fieldManualChapter(String chapterId) {
        if (chapterId == null) {
            return null;
        }
        for (TourBeat beat : fieldManualChapters()) {
            if (chapterId.equals(beat.id)) {
                return beat;
            }
        }
        return null;
    }

    public static int firstRunCount() {
        return firstRunBeats().size();
    }
}
