package com.ruthless.less.friction;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.ruthless.less.LessApplication;
import com.ruthless.less.R;
import com.ruthless.less.applications.AppLauncher;
import com.ruthless.less.database.PolicyRepository;
import com.ruthless.less.database.entities.AppPolicyEntity;
import com.ruthless.less.launcher.LessActivity;
import com.ruthless.less.quota.QuotaManager;
import com.ruthless.less.session.SessionTimerService;
import com.ruthless.less.ui.LessDivider;
import com.ruthless.less.usage.UsageStatsReader;

/**
 * Launch pipeline: quota → confirmation → session duration → delay → launch.
 */
public class LaunchGateActivity extends LessActivity {

    public static final String EXTRA_PACKAGE = "package";
    public static final String EXTRA_LABEL = "label";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final LaunchDelayManager delayManager = new LaunchDelayManager();
    private LinearLayout root;
    private String packageName;
    private String label;
    private boolean cancelled;
    private long sessionDurationMs;

    public static void start(Activity from, String packageName, String label) {
        Intent intent = new Intent(from, LaunchGateActivity.class);
        intent.putExtra(EXTRA_PACKAGE, packageName);
        intent.putExtra(EXTRA_LABEL, label);
        from.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_menu);
        root = findViewById(R.id.menuList);
        View scrim = findViewById(R.id.menuScrim);
        View card = findViewById(R.id.menuCard);
        if (scrim != null) {
            scrim.setOnClickListener(v -> {
                cancelled = true;
                finish();
            });
        }
        if (card != null) {
            card.setOnClickListener(v -> {
                // Keep taps inside the card from dismissing.
            });
        }

        packageName = getIntent().getStringExtra(EXTRA_PACKAGE);
        label = getIntent().getStringExtra(EXTRA_LABEL);
        if (packageName == null) {
            finish();
            return;
        }
        if (label == null) {
            label = packageName.toUpperCase();
        }

        LessApplication app = (LessApplication) getApplication();
        PolicyRepository policies = app.getPolicyRepository();
        UsageStatsReader usage = app.getUsageStatsReader();
        QuotaManager quotas = app.getQuotaManager();

        policies.getOrCreate(packageName, entity -> runOnUiThread(() ->
                beginPipeline(entity, usage, quotas)));
    }

    private void beginPipeline(AppPolicyEntity policy, UsageStatsReader usage, QuotaManager quotas) {
        int used = 0;
        for (UsageStatsReader.UsageRow row : usage.queryToday()) {
            if (packageName.equals(row.packageName)) {
                used = row.minutes();
                break;
            }
        }
        final int usedMinutes = used;
        quotas.evaluate(packageName, usedMinutes, state -> runOnUiThread(() -> {
            if (state.limitMinutes > 0 && state.exhausted) {
                showQuotaFinished(state, quotas);
                return;
            }
            if (policy.launchConfirmationEnabled || policy.isConfused) {
                showConfirmation(policy);
            } else {
                maybeAskSessionThenDelay(policy);
            }
        }));
    }

    private void showQuotaFinished(QuotaManager.QuotaState state, QuotaManager quotas) {
        root.removeAllViews();
        addTitle(label);
        LessDivider.add(this, root);
        LessApplication app = (LessApplication) getApplication();
        if (!app.getSettingsRepository().hasSeenReminder(
                com.ruthless.less.settings.SettingsRepository.REMINDER_QUOTA)) {
            addHint("ONE EXTENSION. MAKE IT DELIBERATE.");
            app.getSettingsRepository().markReminderSeen(
                    com.ruthless.less.settings.SettingsRepository.REMINDER_QUOTA);
        }
        addBody("DAILY QUOTA FINISHED.");
        addBody("USED");
        addBody(state.usedMinutes + " MINUTES");
        LessDivider.add(this, root);
        if (!state.extensionUsed) {
            addAction("EXTEND 5 MINUTES", v -> quotas.requestExtensionAsync(packageName, () ->
                    runOnUiThread(() -> {
                        LessApplication lessApp = (LessApplication) getApplication();
                        lessApp.getPolicyRepository().getOrCreate(packageName, entity ->
                                runOnUiThread(() -> maybeAskSessionThenDelay(entity)));
                    })));
        } else {
            addBody("NO MORE EXTENSIONS");
            addBody("AVAILABLE TODAY.");
            addBody("AVAILABLE AGAIN");
            addBody("TOMORROW.");
            LessDivider.add(this, root);
        }
        addAction(getString(R.string.cancel), v -> finish());
    }

    private void showConfirmation(AppPolicyEntity policy) {
        root.removeAllViews();
        addTitle(label);
        LessDivider.add(this, root);
        LessApplication app = (LessApplication) getApplication();
        if (policy.isConfused) {
            String chide = app.getBoredomMessageManager().nextConfirmChide();
            addHint(chide);
            addBody("");
        }
        addBody("OPEN?");
        LessDivider.add(this, root);
        addAction("YES", v -> maybeAskSessionThenDelay(policy));
        addAction("NO", v -> {
            if (policy.isConfused) {
                String decline = app.getBoredomMessageManager().nextDeclineChide();
                showBrief(decline);
                java.util.List<String> normals = new java.util.ArrayList<>();
                for (com.ruthless.less.applications.InstalledApp a :
                        app.getAppRepository().getLaunchableApps()) {
                    normals.add(a.getPackageName());
                }
                app.getConfuseManager().reshuffleOne(packageName, normals, null);
                handler.postDelayed(this::finish, 900);
            } else {
                finish();
            }
        });
    }

    private void maybeAskSessionThenDelay(AppPolicyEntity policy) {
        if (policy.sessionTimerEnabled) {
            showSessionDurationPicker(policy);
        } else {
            sessionDurationMs = 0L;
            applyDelayThenLaunch(policy);
        }
    }

    private void showSessionDurationPicker(AppPolicyEntity policy) {
        root.removeAllViews();
        addTitle(label);
        LessDivider.add(this, root);
        addBody("HOW LONG?");
        addHint("TIMER RUNS WHILE YOU USE IT.");
        addHint("WHEN TIME IS UP, LESS CLOSES IT.");

        if (!SessionTimerService.canDrawOverlays(this)) {
            addHint("OVERLAY PERMISSION NEEDED");
            LessDivider.add(this, root);
            addAction("ALLOW OVERLAY", v -> SessionTimerService.openOverlaySettings(this));
        }

        LessDivider.add(this, root);
        addAction("1 MINUTE", v -> chooseSession(policy, 60_000L));
        addAction("5 MINUTES", v -> chooseSession(policy, 5 * 60_000L));
        addAction("10 MINUTES", v -> chooseSession(policy, 10 * 60_000L));
        addAction("15 MINUTES", v -> chooseSession(policy, 15 * 60_000L));
        addAction(getString(R.string.cancel), v -> finish());
    }

    private void chooseSession(AppPolicyEntity policy, long durationMs) {
        if (!SessionTimerService.canDrawOverlays(this)) {
            Toast.makeText(this, "ALLOW DISPLAY OVER OTHER APPS", Toast.LENGTH_LONG).show();
            SessionTimerService.openOverlaySettings(this);
            return;
        }
        if (Build.VERSION.SDK_INT >= 33) {
            // Notification permission improves the foreground timer; overlay still works without it.
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 71);
            }
        }
        sessionDurationMs = durationMs;
        applyDelayThenLaunch(policy);
    }

    private void addHint(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextAppearance(this, R.style.LessText_Hint);
        int pad = getResources().getDimensionPixelSize(R.dimen.less_row_padding_compact);
        tv.setPadding(0, pad, 0, pad);
        root.addView(tv);
    }

    private void applyDelayThenLaunch(AppPolicyEntity policy) {
        long delay = delayManager.nextDelayMs(
                policy.launchDelayMode,
                policy.launchDelayMinMs,
                policy.launchDelayMaxMs);
        if (delay < 500) {
            doLaunch();
            return;
        }
        root.removeAllViews();
        addTitle(label);
        LessDivider.add(this, root);
        addBody("OPENING...");
        TextView countdown = addBody(String.format("%02d", (delay + 999) / 1000));
        long endAt = System.currentTimeMillis() + delay;
        Runnable tick = new Runnable() {
            @Override
            public void run() {
                if (cancelled) {
                    return;
                }
                long left = Math.max(0, endAt - System.currentTimeMillis());
                countdown.setText(String.format("%02d", (left + 999) / 1000));
                if (left <= 0) {
                    doLaunch();
                } else {
                    handler.postDelayed(this, 250);
                }
            }
        };
        LessDivider.add(this, root);
        addAction(getString(R.string.cancel), v -> {
            cancelled = true;
            handler.removeCallbacksAndMessages(null);
            finish();
        });
        handler.post(tick);
    }

    private void doLaunch() {
        if (cancelled) {
            return;
        }
        LessApplication app = (LessApplication) getApplication();
        app.getUsageStatsReader().recordLaunchAsync(packageName);
        app.getSettingsRepository().recordAppOpened(packageName, label);
        if (sessionDurationMs > 0L) {
            SessionTimerService.start(this, packageName, label, sessionDurationMs);
        }
        new AppLauncher(this).launch(packageName);
        finish();
    }

    private void showBrief(String message) {
        root.removeAllViews();
        addTitle(message);
    }

    private void addTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextAppearance(this, R.style.LessText_Title);
        tv.setPadding(0, 0, 0, 24);
        root.addView(tv);
    }

    private TextView addBody(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextAppearance(this, R.style.LessText_Body);
        int pad = getResources().getDimensionPixelSize(R.dimen.less_row_padding_compact);
        tv.setPadding(0, pad, 0, pad);
        root.addView(tv);
        return tv;
    }

    private void addAction(String text, View.OnClickListener listener) {
        TextView tv = new TextView(this);
        tv.setText(text);
        com.ruthless.less.ui.LessActionText.styleAsAction(tv, this);
        int pad = getResources().getDimensionPixelSize(R.dimen.less_row_padding_normal);
        tv.setPadding(0, pad, 0, pad);
        tv.setClickable(true);
        tv.setFocusable(true);
        tv.setOnClickListener(listener);
        root.addView(tv);
    }

    @Override
    protected void onDestroy() {
        cancelled = true;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
