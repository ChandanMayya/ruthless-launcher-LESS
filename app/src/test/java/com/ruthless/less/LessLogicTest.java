package com.ruthless.less;

import com.ruthless.less.boredom.BoredomMessageManager;
import com.ruthless.less.confuse.ConfusePlacementEngine;
import com.ruthless.less.friction.LaunchDelayManager;
import com.ruthless.less.usage.PhoneTemperature;
import com.ruthless.less.usage.ScreenTimeAlertLogic;
import com.ruthless.less.usage.UsageStatsReader;
import com.ruthless.less.tour.TourCatalog;
import com.ruthless.less.tour.TourCheck;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LessLogicTest {

    @Test
    public void launchDelayNeverOutsideRange() {
        LaunchDelayManager mgr = new LaunchDelayManager();
        for (int i = 0; i < 200; i++) {
            long d = mgr.nextDelayMs("RANGE", 0, 20_000);
            assertTrue(d >= 0 && d <= 20_000);
        }
        assertEquals(0, mgr.nextDelayMs("OFF", 0, 20_000));
    }

    @Test
    public void customLaunchDelayHonorsBounds() {
        LaunchDelayManager mgr = new LaunchDelayManager();
        for (int i = 0; i < 100; i++) {
            long d = mgr.nextDelayMs("CUSTOM", 2_000, 8_000);
            assertTrue(d >= 2_000 && d <= 8_000);
        }
    }

    @Test
    public void confuseNeverFirstAndNotAdjacent() {
        ConfusePlacementEngine engine = new ConfusePlacementEngine(new Random(42));
        List<String> normals = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            normals.add("normal." + i);
        }
        List<String> confused = Arrays.asList("c.a", "c.b", "c.c");
        Map<String, ConfusePlacementEngine.Placement> placed =
                engine.place(normals, confused, Collections.emptyMap());
        for (ConfusePlacementEngine.Placement p : placed.values()) {
            assertTrue(p.targetPosition >= 1);
            assertTrue(p.targetScroll >= 1 && p.targetScroll <= 3);
        }
        List<String> ordered = engine.materialize(normals, placed);
        assertFalse(confused.contains(ordered.get(0)));
        assertFalse(ConfusePlacementEngine.hasAdjacentConfused(ordered, new HashSet<>(confused)));
    }

    @Test
    public void confusePlacementChangesAcrossSessions() {
        ConfusePlacementEngine engine = new ConfusePlacementEngine(new Random(7));
        List<String> normals = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            normals.add("n." + i);
        }
        List<String> confused = Collections.singletonList("c.x");
        Map<String, Integer> prev = new HashMap<>();
        Map<String, ConfusePlacementEngine.Placement> first = engine.place(normals, confused, prev);
        prev.put("c.x", first.get("c.x").targetPosition);
        boolean changed = false;
        for (int i = 0; i < 20; i++) {
            Map<String, ConfusePlacementEngine.Placement> next = engine.place(normals, confused, prev);
            if (next.get("c.x").targetPosition != prev.get("c.x")) {
                changed = true;
                break;
            }
            prev.put("c.x", next.get("c.x").targetPosition);
        }
        assertTrue(changed);
    }

    @Test
    public void boredomAvoidsRecent() {
        Random random = new Random(1);
        int pool = BoredomMessageManager.homePoolSize();
        List<Integer> recent = Arrays.asList(0, 1, 2, 3, 4);
        for (int i = 0; i < 50; i++) {
            int pick = BoredomMessageManager.pickAvoiding(random, pool, recent);
            assertFalse(recent.contains(pick));
        }
    }

    @Test
    public void boredomPoolsAreLargeEnough() {
        assertTrue(BoredomMessageManager.homePoolSize() >= 100);
        assertTrue(BoredomMessageManager.drawerPoolSize() >= 100);
        assertTrue(BoredomMessageManager.confirmPoolSize() >= 15);
        assertTrue(BoredomMessageManager.declinePoolSize() >= 15);
    }

    @Test
    public void phoneTemperatureBuckets() {
        assertEquals(PhoneTemperature.Level.COLD, PhoneTemperature.fromMinutes(10, 0));
        assertEquals(PhoneTemperature.Level.ON_FIRE, PhoneTemperature.fromMinutes(400, 0));
        assertEquals(PhoneTemperature.Level.HOT, PhoneTemperature.fromMinutes(170, 100));
        assertEquals(PhoneTemperature.Level.ON_FIRE, PhoneTemperature.fromMinutes(200, 100));
        assertEquals("ON FIRE", PhoneTemperature.label(PhoneTemperature.Level.ON_FIRE));
    }

    @Test
    public void quotaExtensionMath() {
        int limit = 20;
        int used = 20;
        boolean extensionUsed = false;
        int extensionMinutes = 0;
        int allowed = limit + (extensionUsed ? extensionMinutes : 0);
        assertTrue(used >= allowed);
        extensionUsed = true;
        extensionMinutes = 5;
        allowed = limit + extensionMinutes;
        assertEquals(25, allowed);
        assertFalse(used >= allowed);
        assertTrue(extensionUsed);
    }

    @Test
    public void formatMinutesAndRelative() {
        assertEquals("20M", UsageStatsReader.formatMinutes(20));
        assertEquals("1H 5M", UsageStatsReader.formatMinutes(65));
        assertEquals("JUST NOW", UsageStatsReader.formatRelative(System.currentTimeMillis()));
        assertEquals("UNKNOWN", UsageStatsReader.formatRelative(0));
    }

    @Test
    public void screenTimeAlertMilestones() {
        assertTrue(ScreenTimeAlertLogic.pendingMilestones(9, 0).isEmpty());
        assertEquals(Arrays.asList(10), ScreenTimeAlertLogic.pendingMilestones(10, 0));
        assertEquals(Arrays.asList(10, 20), ScreenTimeAlertLogic.pendingMilestones(25, 0));
        assertEquals(Arrays.asList(20, 30), ScreenTimeAlertLogic.pendingMilestones(30, 10));
        assertTrue(ScreenTimeAlertLogic.pendingMilestones(30, 30).isEmpty());
        assertEquals("10 MINUTES TODAY", ScreenTimeAlertLogic.bodyFor(10, true));
        assertEquals("20 MINUTES SCREEN ON", ScreenTimeAlertLogic.bodyFor(20, false));
    }

    @Test
    public void tourCatalogIsComplete() {
        assertEquals(9, TourCatalog.firstRunCount());
        assertTrue(TourCatalog.firstRunBeats().get(7).boundaryStep);
        assertEquals(8, TourCatalog.fieldManualChapters().size());
        assertNotNull(TourCatalog.fieldManualChapter(TourCatalog.CHAPTER_CONFUSE));
        assertNotNull(TourCatalog.fieldManualChapter(TourCatalog.CHAPTER_EVIDENCE));
        assertTrue(TourCatalog.fieldManualIndex().size() >= 8);
        assertTrue(TourCatalog.firstRunBeats().get(2).showUsageStatus);
        assertEquals(TourCheck.DEFAULT_HOME, TourCatalog.firstRunBeats().get(1).check);
        assertEquals(TourCheck.USAGE_ACCESS, TourCatalog.firstRunBeats().get(2).check);
        assertEquals(TourCheck.ACCESSIBILITY, TourCatalog.firstRunBeats().get(3).check);
        assertEquals(TourCheck.OVERLAY, TourCatalog.firstRunBeats().get(4).check);
        assertEquals(TourCheck.NOTIFICATIONS, TourCatalog.firstRunBeats().get(5).check);
    }
}
