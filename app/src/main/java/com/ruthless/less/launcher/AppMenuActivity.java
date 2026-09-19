package com.ruthless.less.launcher;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.ruthless.less.LessApplication;
import com.ruthless.less.R;
import com.ruthless.less.applications.AppLauncher;
import com.ruthless.less.database.PolicyRepository;
import com.ruthless.less.friction.LaunchGateActivity;
import com.ruthless.less.settings.SettingsRepository;
import com.ruthless.less.ui.LessDivider;

/**
 * Text-only long-press application menu.
 */
public class AppMenuActivity extends LessActivity {

    public static final String EXTRA_PACKAGE = "package";
    public static final String EXTRA_LABEL = "label";

    private LinearLayout list;
    private String packageName;
    private String label;
    private SettingsRepository settings;
    private PolicyRepository policies;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_menu);

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
        settings = app.getSettingsRepository();
        policies = app.getPolicyRepository();
        list = findViewById(R.id.menuList);

        View scrim = findViewById(R.id.menuScrim);
        View card = findViewById(R.id.menuCard);
        if (scrim != null) {
            scrim.setOnClickListener(v -> finish());
        }
        if (card != null) {
            card.setOnClickListener(v -> {
                // Absorb taps so they do not dismiss via scrim.
            });
        }

        if (!settings.hasSeenReminder(SettingsRepository.REMINDER_LONG_PRESS)) {
            showLongPressReminder();
        } else {
            rebuildMenu(new AppLauncher(this));
        }
    }

    private void showLongPressReminder() {
        list.removeAllViews();
        addTitle(label);
        LessDivider.add(this, list);
        addHint("THIS IS WHERE YOU SET A BOUNDARY.");
        addBody("Limits, confirmation, delay, CONFUSE ME, and session timers live here.");
        LessDivider.add(this, list);
        addAction("CONTINUE", v -> {
            settings.markReminderSeen(SettingsRepository.REMINDER_LONG_PRESS);
            rebuildMenu(new AppLauncher(this));
        });
        addAction("FIELD MANUAL", v -> {
            settings.markReminderSeen(SettingsRepository.REMINDER_LONG_PRESS);
            com.ruthless.less.tour.FieldManualActivity.open(this, com.ruthless.less.tour.TourCatalog.CHAPTER_PAUSE);
            finish();
        });
    }

    private void rebuildMenu(AppLauncher launcher) {
        policies.getOrCreate(packageName, entity -> runOnUiThread(() ->
                rebuildMenuWithPolicy(launcher, entity)));
    }

    private void rebuildMenuWithPolicy(AppLauncher launcher,
                                       com.ruthless.less.database.entities.AppPolicyEntity policy) {
        list.removeAllViews();
        addTitle(label);
        LessDivider.add(this, list);

        LessApplication app = (LessApplication) getApplication();
        boolean confused = app.getAppRepository().isConfused(packageName);

        addAction(getString(R.string.open), v -> {
            LaunchGateActivity.start(this, packageName, label);
            finish();
        });

        if (confused) {
            addHint("CONFUSE ME ON");
            addHint("CANNOT PIN OR ADD TO HOME");
        } else {
            boolean pinned = settings.isPinned(packageName);
            addAction(pinned ? getString(R.string.unpin) : getString(R.string.pin), v -> {
                if (pinned) {
                    settings.unpin(packageName);
                } else {
                    if (app.getAppRepository().isConfused(packageName)) {
                        Toast.makeText(this, "CONFUSE ME APPS\nCANNOT BE PINNED", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    settings.pin(packageName);
                }
                finish();
            });
        }

        LessDivider.addSection(this, list, "BOUNDARIES");
        addAction("APP LIMIT", v -> showLimitPicker());
        addAction("CONFUSE ME", v -> showConfusePicker());
        addAction("LAUNCH CONFIRMATION", v -> showConfirmationPicker());
        addAction("SESSION TIMER", v -> showSessionTimerPicker());
        addAction("LAUNCH DELAY", v -> showDelayPicker());

        boolean monitoring = policy == null || !policy.excludedFromScreenTime;
        addAction(monitoring ? "DISABLE SCREEN TIME MONITORING" : "ENABLE SCREEN TIME MONITORING",
                v -> policies.setExcludedFromScreenTime(packageName, monitoring, () ->
                        runOnUiThread(() -> {
                            Toast.makeText(this,
                                    monitoring ? "SCREEN TIME\nMONITORING OFF" : "SCREEN TIME\nMONITORING ON",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        })));

        LessDivider.addSection(this, list, "PLACEMENT");
        if (confused) {
            if (settings.isHomeApp(packageName)) {
                addAction(getString(R.string.remove_from_home), v -> {
                    settings.removeHomePackage(packageName);
                    finish();
                });
            }
        } else {
            boolean onHome = settings.isHomeApp(packageName);
            addAction(onHome ? getString(R.string.remove_from_home) : getString(R.string.add_to_home),
                    v -> {
                        if (onHome) {
                            settings.removeHomePackage(packageName);
                        } else {
                            if (app.getAppRepository().isConfused(packageName)) {
                                Toast.makeText(this, "CONFUSE ME APPS\nCANNOT BE ON HOME", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            settings.addHomePackage(packageName);
                        }
                        finish();
                    });
        }

        addAction(getString(R.string.app_information), v -> {
            launcher.openAppInfo(packageName);
            finish();
        });

        if (!packageName.equals(getPackageName())) {
            addAction(getString(R.string.uninstall), v -> {
                launcher.uninstall(packageName);
                finish();
            });
        }

        LessDivider.add(this, list);
        addAction(getString(R.string.cancel), v -> finish());
    }

    private void showLimitPicker() {
        list.removeAllViews();
        addTitle(label);
        LessDivider.add(this, list);
        addBody("DAILY LIMIT");
        LessDivider.add(this, list);
        int[] minutes = {0, 15, 30, 45, 60, 120};
        String[] labels = {"NONE", "15 MINUTES", "30 MINUTES", "45 MINUTES", "1 HOUR", "2 HOURS"};
        for (int i = 0; i < minutes.length; i++) {
            final int value = minutes[i];
            addAction(labels[i], v -> policies.setDailyLimitMinutes(packageName, value, () ->
                    runOnUiThread(() -> {
                        Toast.makeText(this, "LIMIT SET", Toast.LENGTH_SHORT).show();
                        finish();
                    })));
        }
        addAction("CUSTOM", v -> showCustomLimit());
        LessDivider.add(this, list);
        addAction(getString(R.string.cancel), v -> rebuildMenu(new AppLauncher(this)));
    }

    private void showCustomLimit() {
        list.removeAllViews();
        addTitle(label);
        LessDivider.add(this, list);
        addBody("CUSTOM LIMIT");
        LessDivider.add(this, list);
        addBody("HOURS");
        EditText hours = addNumberInput("0");
        addBody("MINUTES");
        EditText mins = addNumberInput("20");
        LessDivider.add(this, list);
        addAction("SAVE", v -> {
            int h = parseInt(hours.getText().toString(), 0);
            int m = parseInt(mins.getText().toString(), 0);
            int total = Math.max(0, h * 60 + m);
            policies.setDailyLimitMinutes(packageName, total, () ->
                    runOnUiThread(() -> {
                        Toast.makeText(this, "LIMIT SET\n" + total + " MINUTES", Toast.LENGTH_SHORT).show();
                        finish();
                    }));
        });
        addAction(getString(R.string.cancel), v -> showLimitPicker());
    }

    private void showConfusePicker() {
        list.removeAllViews();
        addTitle(label);
        LessDivider.add(this, list);
        if (!settings.hasSeenReminder(SettingsRepository.REMINDER_CONFUSE)) {
            addHint("EASY TO FIND BECOMES EASY TO OPEN.");
            addBody("Enabling CONFUSE ME will:");
            addBody("• Remove this app from Home");
            addBody("• Remove it from pins");
            addBody("• Hide it from search");
            addBody("Still reversible. Confirmation stays on open.");
            LessDivider.add(this, list);
            addAction("ENABLE ANYWAY", v -> {
                settings.markReminderSeen(SettingsRepository.REMINDER_CONFUSE);
                enableConfuse();
            });
            addAction("FIELD MANUAL", v -> {
                settings.markReminderSeen(SettingsRepository.REMINDER_CONFUSE);
                com.ruthless.less.tour.FieldManualActivity.open(this, com.ruthless.less.tour.TourCatalog.CHAPTER_CONFUSE);
            });
            addAction(getString(R.string.cancel), v -> rebuildMenu(new AppLauncher(this)));
            return;
        }
        addBody("CONFUSE ME");
        addHint("REMOVES FROM HOME AND PINS");
        LessDivider.add(this, list);
        addAction("ENABLE", v -> enableConfuse());
        addAction("DISABLE", v -> policies.setConfused(packageName, false, () ->
                runOnUiThread(() -> {
                    ((LessApplication) getApplication()).getAppRepository().refreshConfusedCache();
                    Toast.makeText(this, "CONFUSE ME\nOFF", Toast.LENGTH_SHORT).show();
                    finish();
                })));
        addAction(getString(R.string.cancel), v -> rebuildMenu(new AppLauncher(this)));
    }

    private void enableConfuse() {
        settings.unpin(packageName);
        settings.removeHomePackage(packageName);
        policies.setConfused(packageName, true, () ->
                runOnUiThread(() -> {
                    ((LessApplication) getApplication()).getAppRepository().refreshConfusedCache();
                    Toast.makeText(this, "CONFUSE ME\nON\nREMOVED FROM HOME/PINS",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }));
    }

    private void showConfirmationPicker() {
        list.removeAllViews();
        addTitle(label);
        LessDivider.add(this, list);
        addBody("LAUNCH CONFIRMATION");
        addBody("OPEN?");
        LessDivider.add(this, list);
        addAction("ENABLE", v -> policies.setLaunchConfirmation(packageName, true, () ->
                runOnUiThread(() -> {
                    Toast.makeText(this, "CONFIRMATION\nON", Toast.LENGTH_SHORT).show();
                    finish();
                })));
        addAction("DISABLE", v -> policies.setLaunchConfirmation(packageName, false, () ->
                runOnUiThread(() -> {
                    Toast.makeText(this, "CONFIRMATION\nOFF", Toast.LENGTH_SHORT).show();
                    finish();
                })));
        addAction(getString(R.string.cancel), v -> rebuildMenu(new AppLauncher(this)));
    }

    private void showSessionTimerPicker() {
        list.removeAllViews();
        addTitle(label);
        LessDivider.add(this, list);
        addBody("SESSION TIMER");
        addHint("ASK HOW LONG ON OPEN.");
        addHint("THEN SHOW A COUNTDOWN.");
        addHint("TIME UP → RETURN HOME.");
        if (!com.ruthless.less.session.SessionTimerService.canDrawOverlays(this)) {
            addHint("OVERLAY PERMISSION OFF");
            LessDivider.add(this, list);
            addAction("ALLOW OVERLAY", v ->
                    com.ruthless.less.session.SessionTimerService.openOverlaySettings(this));
        }
        LessDivider.add(this, list);
        addAction("ENABLE", v -> policies.setSessionTimerEnabled(packageName, true, () ->
                runOnUiThread(() -> {
                    Toast.makeText(this, "SESSION TIMER\nON", Toast.LENGTH_SHORT).show();
                    finish();
                })));
        addAction("DISABLE", v -> policies.setSessionTimerEnabled(packageName, false, () ->
                runOnUiThread(() -> {
                    Toast.makeText(this, "SESSION TIMER\nOFF", Toast.LENGTH_SHORT).show();
                    finish();
                })));
        addAction(getString(R.string.cancel), v -> rebuildMenu(new AppLauncher(this)));
    }

    private void showDelayPicker() {
        list.removeAllViews();
        addTitle(label);
        LessDivider.add(this, list);
        addBody("LAUNCH DELAY");
        addHint("RECOMMENDED: 0–20 SEC");
        LessDivider.add(this, list);
        addAction("OFF", v -> setDelay("OFF", 0, 0));
        addAction("0–5 SEC", v -> setDelay("RANGE", 0, 5_000));
        addAction("0–10 SEC", v -> setDelay("RANGE", 0, 10_000));
        addAction("0–20 SEC", v -> setDelay("RANGE", 0, 20_000));
        addAction("CUSTOM", v -> showCustomDelay());
        LessDivider.add(this, list);
        addAction(getString(R.string.cancel), v -> rebuildMenu(new AppLauncher(this)));
    }

    private void showCustomDelay() {
        list.removeAllViews();
        addTitle(label);
        LessDivider.add(this, list);
        addBody("CUSTOM DELAY");
        LessDivider.add(this, list);
        addBody("MIN SECONDS");
        EditText min = addNumberInput("0");
        addBody("MAX SECONDS");
        EditText max = addNumberInput("20");
        LessDivider.add(this, list);
        addAction("SAVE", v -> {
            int minSec = Math.max(0, parseInt(min.getText().toString(), 0));
            int maxSec = Math.max(minSec, parseInt(max.getText().toString(), 20));
            setDelay("CUSTOM", minSec * 1000, maxSec * 1000);
        });
        addAction(getString(R.string.cancel), v -> showDelayPicker());
    }

    private void setDelay(String mode, int min, int max) {
        policies.setLaunchDelayRange(packageName, mode, min, max, () ->
                runOnUiThread(() -> {
                    if (!"OFF".equals(mode)) {
                        policies.setLaunchConfirmation(packageName, true, null);
                    }
                    Toast.makeText(this, "DELAY SET", Toast.LENGTH_SHORT).show();
                    finish();
                }));
    }

    private EditText addNumberInput(String initial) {
        EditText et = new EditText(this);
        et.setText(initial);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setTextAppearance(this, R.style.LessText_Body);
        et.setTextColor(getResources().getColor(R.color.less_white));
        et.setHintTextColor(0x88FFFFFF);
        et.setBackgroundColor(getResources().getColor(R.color.less_black));
        int pad = getResources().getDimensionPixelSize(R.dimen.less_row_padding_normal);
        et.setPadding(0, pad, 0, pad);
        list.addView(et);
        return et;
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private void addTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextAppearance(this, R.style.LessText_Title);
        tv.setPadding(0, 0, 0, 8);
        tv.setContentDescription(text);
        list.addView(tv);
    }

    private void addBody(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextAppearance(this, R.style.LessText_Body);
        int pad = getResources().getDimensionPixelSize(R.dimen.less_row_padding_compact);
        tv.setPadding(0, pad, 0, pad);
        list.addView(tv);
    }

    private void addHint(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextAppearance(this, R.style.LessText_Hint);
        int pad = getResources().getDimensionPixelSize(R.dimen.less_row_padding_compact);
        tv.setPadding(0, pad, 0, pad);
        list.addView(tv);
    }

    private void addAction(String text, View.OnClickListener listener) {
        TextView tv = new TextView(this);
        tv.setText(text);
        com.ruthless.less.ui.LessActionText.styleAsAction(tv, this);
        int pad = getResources().getDimensionPixelSize(R.dimen.less_row_padding_normal);
        tv.setPadding(0, pad, 0, pad);
        tv.setClickable(true);
        tv.setFocusable(true);
        tv.setContentDescription(text);
        tv.setOnClickListener(listener);
        list.addView(tv);
    }
}
