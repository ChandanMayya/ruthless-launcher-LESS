package com.ruthless.less.applications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.ruthless.less.confuse.ConfuseManager;
import com.ruthless.less.confuse.ConfusePlacementEngine;
import com.ruthless.less.database.PolicyRepository;
import com.ruthless.less.settings.SettingsRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Discovers launchable apps via PackageManager.
 * Excludes LESS itself. Does not load icons.
 */
public final class AppRepository {

    public interface Listener {
        void onAppsChanged();
    }

    private final Context context;
    private final SettingsRepository settings;
    private final ConfuseManager confuseManager;
    private final PolicyRepository policies;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private volatile List<InstalledApp> cachedApps = Collections.emptyList();
    private volatile Map<String, InstalledApp> byPackage = Collections.emptyMap();
    private volatile Set<String> confusedPackages = Collections.emptySet();
    private BroadcastReceiver packageReceiver;

    public AppRepository(
            Context context,
            SettingsRepository settings,
            ConfuseManager confuseManager,
            PolicyRepository policies) {
        this.context = context.getApplicationContext();
        this.settings = settings;
        this.confuseManager = confuseManager;
        this.policies = policies;
        refreshAsync();
        registerPackageChanges();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public List<InstalledApp> getLaunchableApps() {
        return cachedApps;
    }

    public InstalledApp findByPackage(String packageName) {
        return byPackage.get(packageName);
    }

    public boolean isConfused(String packageName) {
        return packageName != null && confusedPackages.contains(packageName);
    }

    public void refreshAsync() {
        executor.execute(() -> {
            List<InstalledApp> apps = loadLaunchableApps();
            Map<String, InstalledApp> map = new HashMap<>();
            for (InstalledApp app : apps) {
                map.put(app.getPackageName(), app);
            }
            cachedApps = apps;
            byPackage = map;
            try {
                confusedPackages = confuseManager.getConfusedPackagesBlocking();
            } catch (Exception ignored) {
                confusedPackages = Collections.emptySet();
            }
            for (Listener listener : listeners) {
                listener.onAppsChanged();
            }
        });
    }

    public void refreshConfusedCache() {
        executor.execute(() -> {
            try {
                confusedPackages = confuseManager.getConfusedPackagesBlocking();
            } catch (Exception ignored) {
                confusedPackages = Collections.emptySet();
            }
            // CONFUSE ME apps must not remain on home or pinned.
            for (String pkg : confusedPackages) {
                if (settings.isPinned(pkg)) {
                    settings.unpin(pkg);
                }
                if (settings.isHomeApp(pkg)) {
                    settings.removeHomePackage(pkg);
                }
            }
            for (Listener listener : listeners) {
                listener.onAppsChanged();
            }
        });
    }

    /**
     * Drawer ordering: pinned first, then normals with CONFUSE ME injected.
     * Confused apps never appear in search results.
     */
    public List<DrawerItem> buildDrawerItems(String query) {
        List<String> pinnedOrder = settings.getPinnedPackages();
        Set<String> pinnedSet = new HashSet<>(pinnedOrder);
        String q = query == null ? "" : query.trim().toUpperCase(Locale.US);
        Set<String> confused = confusedPackages;

        List<InstalledApp> pinned = new ArrayList<>();
        for (String pkg : pinnedOrder) {
            InstalledApp app = byPackage.get(pkg);
            if (app != null && matches(app, q) && !confused.contains(pkg)) {
                pinned.add(app);
            }
        }

        List<InstalledApp> others = new ArrayList<>();
        for (InstalledApp app : cachedApps) {
            if (pinnedSet.contains(app.getPackageName())) {
                continue;
            }
            if (confused.contains(app.getPackageName())) {
                // Hidden from search entirely.
                if (!q.isEmpty()) {
                    continue;
                }
                continue; // placed via confuse engine below when not searching
            }
            if (matches(app, q)) {
                others.add(app);
            }
        }

        List<DrawerItem> items = new ArrayList<>();
        if (!q.isEmpty()) {
            // Searching a confused app name must look like a miss — never reveal it exists.
            if (pinned.isEmpty() && others.isEmpty()) {
                items.add(DrawerItem.empty("NO APPLICATION FOUND"));
                return items;
            }
            for (InstalledApp app : pinned) {
                items.add(DrawerItem.app(app));
            }
            for (InstalledApp app : others) {
                items.add(DrawerItem.app(app));
            }
            return items;
        }

        if (!pinned.isEmpty()) {
            items.add(DrawerItem.section("PINNED"));
            for (InstalledApp app : pinned) {
                items.add(DrawerItem.app(app));
            }
        }
        items.add(DrawerItem.section("ALL APPLICATIONS"));

        List<String> normalPkgs = new ArrayList<>();
        for (InstalledApp app : others) {
            normalPkgs.add(app.getPackageName());
        }
        List<String> confusedList = new ArrayList<>();
        for (String pkg : confused) {
            if (!pinnedSet.contains(pkg) && byPackage.containsKey(pkg)) {
                confusedList.add(pkg);
            }
        }

        Map<String, ConfusePlacementEngine.Placement> placements =
                confuseManager.getCurrentPlacements();
        List<String> ordered;
        if (placements.isEmpty() && !confusedList.isEmpty()) {
            // Session not ready yet — show normals only; confused appear after session start.
            ordered = normalPkgs;
        } else {
            ordered = confuseManager.materialize(normalPkgs);
            if (ordered.isEmpty()) {
                ordered = normalPkgs;
            }
        }

        for (String pkg : ordered) {
            if (confused.contains(pkg) && confuseManager.getExposureTracker().shouldHide(pkg)) {
                continue;
            }
            InstalledApp app = byPackage.get(pkg);
            if (app != null) {
                items.add(DrawerItem.app(app));
            }
        }
        return items;
    }

    public List<InstalledApp> resolveHomeApps() {
        List<InstalledApp> result = new ArrayList<>();
        for (String pkg : settings.getHomePackages()) {
            if (confusedPackages.contains(pkg)) {
                continue;
            }
            InstalledApp app = byPackage.get(pkg);
            if (app != null) {
                result.add(app);
            }
        }
        return result;
    }

    private static boolean matches(InstalledApp app, String q) {
        if (q.isEmpty()) {
            return true;
        }
        return app.getLabelNormalized().contains(q)
                || app.getPackageName().toUpperCase(Locale.US).contains(q);
    }

    private List<InstalledApp> loadLaunchableApps() {
        PackageManager pm = context.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolved = pm.queryIntentActivities(main, 0);
        Map<String, InstalledApp> unique = new HashMap<>();
        String self = context.getPackageName();

        for (ResolveInfo info : resolved) {
            if (info.activityInfo == null) {
                continue;
            }
            String pkg = info.activityInfo.packageName;
            if (self.equals(pkg)) {
                continue;
            }
            if (unique.containsKey(pkg)) {
                continue;
            }
            CharSequence labelCs = info.loadLabel(pm);
            String label = labelCs == null ? pkg : labelCs.toString().trim();
            if (label.isEmpty()) {
                label = pkg;
            }
            unique.put(pkg, new InstalledApp(pkg, label));
        }

        List<InstalledApp> apps = new ArrayList<>(unique.values());
        Collections.sort(apps, Comparator.comparing(InstalledApp::getLabelNormalized)
                .thenComparing(InstalledApp::getPackageName));
        return apps;
    }

    private void registerPackageChanges() {
        packageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                String pkg = intent.getData() != null ? intent.getData().getSchemeSpecificPart() : null;
                refreshAsync();
                if (pkg != null
                        && (Intent.ACTION_PACKAGE_ADDED.equals(action)
                        || Intent.ACTION_PACKAGE_REPLACED.equals(action))) {
                    executor.execute(() -> {
                        if (policies.hasNonDefaultPolicyBlocking(pkg)) {
                            mainHandler.post(() -> Toast.makeText(
                                    AppRepository.this.context,
                                    "POLICY RESTORED\n" + pkg.toUpperCase(Locale.US),
                                    Toast.LENGTH_LONG).show());
                        }
                    });
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(packageReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(packageReceiver, filter);
        }
    }

    public static String[] defaultCandidatesFor(String role) {
        switch (role) {
            case "PHONE":
                return new String[]{
                        "com.google.android.dialer",
                        "com.android.dialer",
                        "com.samsung.android.dialer",
                        "com.android.phone"
                };
            case "MESSAGES":
                return new String[]{
                        "com.google.android.apps.messaging",
                        "com.android.mms",
                        "com.samsung.android.messaging",
                        "com.android.messaging"
                };
            case "CONTACTS":
                return new String[]{
                        "com.google.android.contacts",
                        "com.android.contacts",
                        "com.samsung.android.contacts"
                };
            case "CAMERA":
                return new String[]{
                        "com.google.android.GoogleCamera",
                        "com.android.camera2",
                        "com.android.camera",
                        "com.samsung.android.camera"
                };
            case "CALENDAR":
                return new String[]{
                        "com.google.android.calendar",
                        "com.android.calendar",
                        "com.samsung.android.calendar"
                };
            case "MUSIC":
                return new String[]{
                        "com.google.android.apps.youtube.music",
                        "com.google.android.music",
                        "com.spotify.music",
                        "com.apple.android.music",
                        "com.samsung.android.app.music.chn",
                        "com.samsung.android.app.music",
                        "com.android.music"
                };
            default:
                return new String[0];
        }
    }

    public String resolveDefaultPackage(String role) {
        for (String candidate : defaultCandidatesFor(role)) {
            if (byPackage.containsKey(candidate)) {
                return candidate;
            }
        }
        String needle = role.toUpperCase(Locale.US);
        for (InstalledApp app : cachedApps) {
            if (app.getLabelNormalized().contains(needle)) {
                return app.getPackageName();
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    private static boolean isSystemApp(ApplicationInfo info) {
        return (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }
}
