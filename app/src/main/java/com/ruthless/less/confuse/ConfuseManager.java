package com.ruthless.less.confuse;

import com.ruthless.less.database.AppDatabase;
import com.ruthless.less.database.dao.AppPolicyDao;
import com.ruthless.less.database.dao.ConfusePlacementDao;
import com.ruthless.less.database.entities.AppPolicyEntity;
import com.ruthless.less.database.entities.ConfusePlacementEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Orchestrates CONFUSE ME for a drawer session.
 */
public final class ConfuseManager {

    private final AppPolicyDao policyDao;
    private final ConfusePlacementDao placementDao;
    private final ConfusePlacementEngine engine = new ConfusePlacementEngine();
    private final ScrollExposureTracker exposureTracker = new ScrollExposureTracker();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String sessionId;
    private Map<String, ConfusePlacementEngine.Placement> current = new HashMap<>();
    private final Map<String, Integer> previousPositions = new HashMap<>();

    public ConfuseManager(AppDatabase database) {
        this.policyDao = database.appPolicyDao();
        this.placementDao = database.confusePlacementDao();
    }

    public ScrollExposureTracker getExposureTracker() {
        return exposureTracker;
    }

    public Set<String> getConfusedPackagesBlocking() {
        Set<String> set = new HashSet<>();
        for (AppPolicyEntity e : policyDao.getConfused()) {
            set.add(e.packageName);
        }
        return set;
    }

    public void startSessionAsync(List<String> normalOrdered, Runnable onReady) {
        executor.execute(() -> {
            List<String> confused = new ArrayList<>(getConfusedPackagesBlocking());
            Map<String, ConfusePlacementEngine.Placement> placed =
                    engine.place(normalOrdered, confused, previousPositions);
            for (ConfusePlacementEngine.Placement p : placed.values()) {
                previousPositions.put(p.packageName, p.targetPosition);
            }
            // Clear the previous session (or all rows) before creating a new id.
            String previous = sessionId;
            sessionId = UUID.randomUUID().toString();
            if (previous != null) {
                placementDao.clearSession(previous);
            } else {
                placementDao.clearAll();
            }
            persistPlacements(placed);
            current = placed;
            exposureTracker.beginSession(placed);
            if (onReady != null) {
                onReady.run();
            }
        });
    }

    public Map<String, ConfusePlacementEngine.Placement> getCurrentPlacements() {
        return current;
    }

    public List<String> materialize(List<String> normalOrdered) {
        return engine.materialize(normalOrdered, current);
    }

    /** After user taps NO on a confused app, reshuffle that app only. */
    public void reshuffleOne(String packageName, List<String> normalOrdered, Runnable onReady) {
        executor.execute(() -> {
            List<String> confused = new ArrayList<>(getConfusedPackagesBlocking());
            if (!confused.contains(packageName)) {
                if (onReady != null) onReady.run();
                return;
            }
            Map<String, Integer> avoid = new HashMap<>(previousPositions);
            Map<String, ConfusePlacementEngine.Placement> placed =
                    engine.place(normalOrdered, confused, avoid);
            current = placed;
            for (ConfusePlacementEngine.Placement p : placed.values()) {
                previousPositions.put(p.packageName, p.targetPosition);
            }
            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString();
            } else {
                placementDao.clearSession(sessionId);
            }
            persistPlacements(placed);
            exposureTracker.beginSession(placed);
            if (onReady != null) onReady.run();
        });
    }

    private void persistPlacements(Map<String, ConfusePlacementEngine.Placement> placed) {
        for (ConfusePlacementEngine.Placement p : placed.values()) {
            ConfusePlacementEntity row = new ConfusePlacementEntity();
            row.packageName = p.packageName;
            row.sessionId = sessionId;
            row.targetScroll = p.targetScroll;
            row.targetPosition = p.targetPosition;
            Integer prev = previousPositions.get(p.packageName);
            row.previousPosition = prev == null ? -1 : prev;
            placementDao.upsert(row);
        }
    }

    public boolean isConfused(String packageName) {
        return current.containsKey(packageName) || getConfusedPackagesBlocking().contains(packageName);
    }
}
