package com.ruthless.less.launcher;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ruthless.less.LessApplication;
import com.ruthless.less.R;
import com.ruthless.less.accessibility.UsageEnforcementAccessibilityService;
import com.ruthless.less.applications.AppRepository;
import com.ruthless.less.applications.InstalledApp;
import com.ruthless.less.friction.LaunchGateActivity;
import com.ruthless.less.settings.OnboardingActivity;
import com.ruthless.less.settings.SettingsActivity;
import com.ruthless.less.settings.SettingsRepository;
import com.ruthless.less.usage.PhoneTemperature;
import com.ruthless.less.usage.ScreenTimeActivity;
import com.ruthless.less.usage.UsageRepository;
import com.ruthless.less.usage.UsageStatsReader;

/**
 * HOME / DEFAULT launcher activity. Text-only black/white UI.
 */
public class LauncherActivity extends LessActivity implements AppRepository.Listener {

    private AppRepository appRepository;
    private SettingsRepository settings;
    private HomeController homeController;
    private AppDrawerController drawerController;
    private HomeAppsAdapter homeAppsAdapter;
    private GestureDetectorCompat gestureDetector;
    private TextView screenTimeText;
    private TextView temperatureText;
    private TextView pickupsText;
    private TextView lastUnlockText;
    private TextView boredomText;
    private LessApplication lessApp;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        lessApp = (LessApplication) getApplication();
        appRepository = lessApp.getAppRepository();
        settings = lessApp.getSettingsRepository();

        if (!settings.isOnboardingDone()) {
            Intent onboarding = new Intent(this, OnboardingActivity.class);
            onboarding.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(onboarding);
        }

        TextView clock = findViewById(R.id.clockText);
        TextView weekday = findViewById(R.id.dateWeekdayText);
        TextView date = findViewById(R.id.dateFullText);
        TextView battery = findViewById(R.id.batteryText);
        android.widget.ImageView batteryFlash = findViewById(R.id.batteryFlash);
        screenTimeText = findViewById(R.id.screenTimeText);
        temperatureText = findViewById(R.id.temperatureText);
        pickupsText = findViewById(R.id.pickupsText);
        lastUnlockText = findViewById(R.id.lastUnlockText);
        boredomText = findViewById(R.id.boredomText);
        homeController = new HomeController(this, clock, weekday, date, battery, batteryFlash);

        RecyclerView homeList = findViewById(R.id.homeAppsList);
        homeAppsAdapter = new HomeAppsAdapter(new HomeAppsAdapter.Callbacks() {
            @Override
            public void onAppClick(InstalledApp installedApp) {
                launchApp(installedApp);
            }

            @Override
            public void onAppLongClick(InstalledApp installedApp) {
                openAppMenu(installedApp);
            }
        });
        homeList.setLayoutManager(new LinearLayoutManager(this));
        homeList.setAdapter(homeAppsAdapter);

        clock.setOnClickListener(v -> openClockApp());
        clock.setOnLongClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        });
        screenTimeText.setOnClickListener(v ->
                startActivity(new Intent(this, ScreenTimeActivity.class)));
        pickupsText.setOnClickListener(v ->
                startActivity(new Intent(this, ScreenTimeActivity.class)));

        View homePanel = findViewById(R.id.homePanel);
        View drawerPanel = findViewById(R.id.drawerPanel);
        EditText search = findViewById(R.id.searchInput);
        RecyclerView drawerList = findViewById(R.id.drawerAppsList);

        drawerController = new AppDrawerController(
                homePanel,
                drawerPanel,
                search,
                findViewById(R.id.drawerChidingText),
                drawerList,
                appRepository,
                settings,
                lessApp.getConfuseManager(),
                lessApp.getBoredomMessageManager(),
                new AppDrawerController.Host() {
                    @Override
                    public void onAppClick(InstalledApp installedApp) {
                        launchApp(installedApp);
                    }

                    @Override
                    public void onAppLongClick(InstalledApp installedApp) {
                        openAppMenu(installedApp);
                    }
                });

        gestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 80;
            private static final int SWIPE_VELOCITY = 80;

            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                if (drawerController != null && !drawerController.isOpen()) {
                    lockPhoneFromHome();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) {
                    return false;
                }
                float dy = e1.getY() - e2.getY();
                float dx = Math.abs(e1.getX() - e2.getX());
                // Swipe up — open/close app drawer
                if (dy > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY && dy > dx) {
                    if (drawerController.isOpen()) {
                        drawerController.close();
                    } else {
                        drawerController.open();
                    }
                    return true;
                }
                // Swipe down
                if (-dy > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY && -dy > dx) {
                    if (drawerController.isOpen()) {
                        drawerController.close();
                        return true;
                    }
                    // Home empty space: left half → notification shade, right half → quick settings
                    openSystemPanelFromSwipe(e1.getRawX());
                    return true;
                }
                return false;
            }
        });

        View root = findViewById(R.id.root);
        View.OnTouchListener homeGesture = (v, event) -> {
            gestureDetector.onTouchEvent(event);
            // Do not consume — children (apps, buttons) keep working.
            return false;
        };
        root.setOnTouchListener(homeGesture);
        homePanel.setOnTouchListener(homeGesture);
        // Double-tap lock only on empty list space, not on app rows.
        homeList.setOnTouchListener((v, event) -> {
            View child = homeList.findChildViewUnder(event.getX(), event.getY());
            if (child == null) {
                gestureDetector.onTouchEvent(event);
            }
            return false;
        });
        // Non-interactive chrome — double-tap here also locks.
        int[] lockTapIds = {
                R.id.dateWeekdayText,
                R.id.dateFullText,
                R.id.temperatureHeader,
                R.id.temperatureText,
                R.id.pickupsHeader,
                R.id.lastUnlockHeader,
                R.id.lastUnlockText,
                R.id.boredomText,
                R.id.homeAppsHeader
        };
        for (int id : lockTapIds) {
            View v = findViewById(id);
            if (v != null) {
                v.setOnTouchListener(homeGesture);
            }
        }
        drawerPanel.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });
        findViewById(R.id.swipeHint).setOnClickListener(v -> drawerController.open());

        appRepository.addListener(this);
    }

    private void lockPhoneFromHome() {
        if (UsageEnforcementAccessibilityService.lockScreen()) {
            return;
        }
        Toast.makeText(this, R.string.double_tap_lock_need_accessibility, Toast.LENGTH_LONG).show();
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception ignored) {
        }
    }

    /**
     * Swipe down on home: left side opens notifications, right side opens quick settings.
     */
    private void openSystemPanelFromSwipe(float rawX) {
        int width = getResources().getDisplayMetrics().widthPixels;
        boolean leftHalf = rawX < (width / 2f);
        boolean ok = leftHalf
                ? com.ruthless.less.system.SystemPanels.openNotifications(this)
                : com.ruthless.less.system.SystemPanels.openQuickSettings(this);
        if (!ok) {
            Toast.makeText(this, R.string.swipe_down_panels_need_accessibility, Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * System Home / gesture-up re-delivers MAIN+HOME to this singleTask launcher.
     * Without closing the drawer, the user stays stuck on APPLICATIONS.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (drawerController != null && drawerController.isOpen()) {
            drawerController.close();
        }
    }

    private void launchApp(InstalledApp app) {
        LaunchGateActivity.start(this, app.getPackageName(), app.getDisplayLabel());
    }

    private void openClockApp() {
        try {
            Intent alarms = new Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS);
            alarms.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(alarms);
            return;
        } catch (Exception ignored) {
            // Fall through.
        }
        try {
            Intent clock = Intent.makeMainSelectorActivity(
                    Intent.ACTION_MAIN, "android.intent.category.APP_CLOCK");
            clock.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(clock);
        } catch (Exception e) {
            Toast.makeText(this, R.string.cannot_launch, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        homeController.start();
        appRepository.refreshAsync();
        bindHomeApps();
        refreshUsageChrome();
        refreshLastUnlock();
        lessApp.getUsageStatsReader().snapshotTodayAsync();
        com.ruthless.less.usage.ScreenTimeAlertMonitor.get(this).checkNow();
        // Returning to home leaves any timed app session.
        if (com.ruthless.less.session.SessionTimerService.hasActiveSession()) {
            com.ruthless.less.session.SessionTimerService.onForegroundPackage(
                    this, getPackageName());
        }
        settings.recordUnlock();
        showBoredomMessage();
        if (drawerController.isOpen()) {
            drawerController.refresh();
        }
    }

    @Override
    protected void onPause() {
        homeController.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        appRepository.removeListener(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (drawerController.isOpen()) {
            drawerController.close();
        }
    }

    @Override
    public void onAppsChanged() {
        runOnUiThread(() -> {
            settings.seedDefaultsIfNeeded(appRepository);
            bindHomeApps();
            if (drawerController.isOpen()) {
                drawerController.refresh();
            }
        });
    }

    private void bindHomeApps() {
        homeAppsAdapter.setApps(appRepository.resolveHomeApps());
    }

    private void refreshUsageChrome() {
        UsageStatsReader reader = lessApp.getUsageStatsReader();
        if (!reader.hasUsageAccess()) {
            screenTimeText.setText("USAGE ACCESS OFF");
            temperatureText.setText("—");
            pickupsText.setText("—");
            return;
        }

        int pickups = reader.countPickupsForDayOffset(0);
        if (pickups >= 0) {
            pickupsText.setText(String.valueOf(pickups));
        } else {
            pickupsText.setText("—");
        }

        reader.todayTotalMsAsync(true, totalMs -> runOnUiThread(() -> {
            int todayMinutes = (int) (totalMs / 60_000L);
            screenTimeText.setText(UsageStatsReader.formatDuration(totalMs));

            lessApp.getUsageRepository().loadLastDays(7, true, days -> runOnUiThread(() -> {
                int sum = 0;
                int n = 0;
                for (UsageRepository.DayTotal day : days) {
                    if (n == 0) {
                        n++;
                        continue;
                    }
                    sum += day.totalMinutes;
                    n++;
                }
                int avg = (n > 1) ? sum / (n - 1) : 0;
                PhoneTemperature.Level level = PhoneTemperature.fromMinutes(todayMinutes, avg);
                temperatureText.setText(PhoneTemperature.label(level));
            }));
        }));
    }

    private void refreshLastUnlock() {
        String label = settings.getLastOpenedLabel();
        long openedAt = settings.getLastOpenedAt();
        if (openedAt > 0 && label != null && !label.isEmpty()) {
            int used = lessApp.getUsageStatsReader().minutesForPackageToday(
                    settings.getLastOpenedPackage());
            lastUnlockText.setText(label + "\nUSED  "
                    + UsageStatsReader.formatMinutes(Math.max(used, 0))
                    + "\n"
                    + UsageStatsReader.formatRelative(openedAt));
        } else {
            long lastUnlock = settings.getLastUnlockAt();
            if (lastUnlock > 0) {
                lastUnlockText.setText(UsageStatsReader.formatRelative(lastUnlock)
                        + "\nNOTHING WAS OPENED.");
            } else {
                lastUnlockText.setText("—");
            }
        }
    }

    private void showBoredomMessage() {
        if (!settings.isBoredomModeEnabled()) {
            boredomText.setText("");
            return;
        }
        lessApp.getBoredomMessageManager().nextMessageAsync(message ->
                runOnUiThread(() -> boredomText.setText(message)));
    }

    private void openAppMenu(InstalledApp app) {
        Intent intent = new Intent(this, AppMenuActivity.class);
        intent.putExtra(AppMenuActivity.EXTRA_PACKAGE, app.getPackageName());
        intent.putExtra(AppMenuActivity.EXTRA_LABEL, app.getDisplayLabel());
        startActivity(intent);
    }
}
