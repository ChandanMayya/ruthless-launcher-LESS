package com.ruthless.less.confuse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * CONFUSE ME placement engine.
 * Normal apps stay predictable; only confused apps are relocated.
 */
public final class ConfusePlacementEngine {

    public static final class Placement {
        public final String packageName;
        public final int targetScroll; // 1..3
        public final int targetPosition; // index in final list, never 0
        public String exposure = "HIDDEN"; // HIDDEN | ELIGIBLE | REVEALED

        public Placement(String packageName, int targetScroll, int targetPosition) {
            this.packageName = packageName;
            this.targetScroll = targetScroll;
            this.targetPosition = targetPosition;
        }
    }

    private final Random random;

    public ConfusePlacementEngine() {
        this(new Random());
    }

    public ConfusePlacementEngine(Random random) {
        this.random = random;
    }

    /**
     * @param normalOrdered alphabetical (or pinned-excluded) package list without confused apps
     * @param confusedPackages packages marked CONFUSE ME
     * @param previousPositions prior session positions to avoid when possible
     */
    public Map<String, Placement> place(
            List<String> normalOrdered,
            List<String> confusedPackages,
            Map<String, Integer> previousPositions) {

        Map<String, Placement> result = new HashMap<>();
        if (confusedPackages == null || confusedPackages.isEmpty()) {
            return result;
        }

        Set<String> confusedSet = new HashSet<>(confusedPackages);
        List<String> normals = new ArrayList<>();
        if (normalOrdered != null) {
            for (String pkg : normalOrdered) {
                if (!confusedSet.contains(pkg)) {
                    normals.add(pkg);
                }
            }
        }

        int baseSize = normals.size();
        int totalSize = baseSize + confusedPackages.size();
        Set<Integer> taken = new HashSet<>();
        // Never first.
        taken.add(0);

        for (String pkg : confusedPackages) {
            int scroll = 1 + random.nextInt(3);
            Integer prev = previousPositions == null ? null : previousPositions.get(pkg);
            int pos = pickPosition(totalSize, taken, prev);
            taken.add(pos);
            // Enforce non-adjacency: mark neighbors reserved for later confused apps.
            taken.add(pos - 1);
            taken.add(pos + 1);
            result.put(pkg, new Placement(pkg, scroll, pos));
        }
        return result;
    }

    private int pickPosition(int totalSize, Set<Integer> taken, Integer previous) {
        List<Integer> candidates = new ArrayList<>();
        for (int i = 1; i < Math.max(2, totalSize); i++) {
            if (!taken.contains(i) && (previous == null || i != previous)) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) {
            for (int i = 1; i < Math.max(2, totalSize); i++) {
                if (!taken.contains(i)) {
                    candidates.add(i);
                }
            }
        }
        if (candidates.isEmpty()) {
            return Math.max(1, totalSize - 1);
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * Build final ordered package list: normals in order with confused inserted at positions.
     */
    public List<String> materialize(List<String> normalOrdered, Map<String, Placement> placements) {
        List<String> normals = new ArrayList<>(
                normalOrdered == null ? java.util.Collections.emptyList() : normalOrdered);
        int total = normals.size() + placements.size();
        String[] slots = new String[Math.max(total, 1)];
        for (Placement p : placements.values()) {
            int idx = Math.min(Math.max(1, p.targetPosition), total - 1);
            while (idx < total && slots[idx] != null) {
                idx++;
            }
            if (idx >= total) {
                idx = total - 1;
                while (idx > 0 && slots[idx] != null) {
                    idx--;
                }
            }
            slots[idx] = p.packageName;
        }
        int n = 0;
        for (int i = 0; i < total; i++) {
            if (slots[i] == null) {
                if (n < normals.size()) {
                    slots[i] = normals.get(n++);
                }
            }
        }
        List<String> out = new ArrayList<>();
        for (String s : slots) {
            if (s != null) {
                out.add(s);
            }
        }
        return out;
    }

    public static boolean hasAdjacentConfused(List<String> ordered, Set<String> confused) {
        String prev = null;
        for (String pkg : ordered) {
            if (confused.contains(pkg) && prev != null && confused.contains(prev)) {
                return true;
            }
            prev = pkg;
        }
        return false;
    }
}
