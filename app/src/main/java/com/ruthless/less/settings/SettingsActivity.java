package com.ruthless.less.settings;

import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.ruthless.less.LessApplication;
import com.ruthless.less.R;
import com.ruthless.less.launcher.LessActivity;
import com.ruthless.less.ui.LessDivider;
import com.ruthless.less.usage.ScreenTimeActivity;

/**
 * Text-only settings root.
 */
public class SettingsActivity extends LessActivity {

    private SettingsRepository settings;
    private LinearLayout list;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settings = ((LessApplication) getApplication()).getSettingsRepository();
        list = findViewById(R.id.settingsList);
        rebuild();
    }

    @Override
    protected void onResume() {
        super.onResume();
        rebuild();
    }

    private void rebuild() {
        list.removeAllViews();
        addTitle(list, getString(R.string.settings));

        LessDivider.addSection(this, list, "GUIDE");
        addRow(list, "FIELD MANUAL", v ->
                com.ruthless.less.tour.FieldManualActivity.open(this, null));

        LessDivider.addSection(this, list, "HOME");
        addRow(list, getString(R.string.home_apps), v ->
                startActivity(new android.content.Intent(this, HomeAppsActivity.class)));
        addRow(list, getString(R.string.pinned_apps), v ->
                startActivity(new android.content.Intent(this, PinnedAppsActivity.class)));

        LessDivider.addSection(this, list, "BOUNDARIES");
        addRow(list, "APP LIMITS", v ->
                PolicyListActivity.open(this, com.ruthless.less.database.PolicyRepository.PolicyFilter.LIMITS));
        addRow(list, "CONFUSE ME", v ->
                PolicyListActivity.open(this, com.ruthless.less.database.PolicyRepository.PolicyFilter.CONFUSE));
        addRow(list, "LAUNCH DELAYS", v ->
                PolicyListActivity.open(this, com.ruthless.less.database.PolicyRepository.PolicyFilter.DELAYS));
        addRow(list, "LAUNCH CONFIRMATION", v ->
                PolicyListActivity.open(this, com.ruthless.less.database.PolicyRepository.PolicyFilter.CONFIRMATION));
        addRow(list, "SESSION TIMERS", v ->
                PolicyListActivity.open(this, com.ruthless.less.database.PolicyRepository.PolicyFilter.SESSION_TIMER));
        addRow(list, "SCREEN TIME OFF", v ->
                PolicyListActivity.open(this, com.ruthless.less.database.PolicyRepository.PolicyFilter.EXCLUSIONS));

        LessDivider.addSection(this, list, "RECEIPTS");
        addRow(list, "SCREEN TIME", v ->
                startActivity(new android.content.Intent(this, ScreenTimeActivity.class)));
        addRow(list, "PHONE TEMPERATURE", v -> showTemperatureInfo());
        addRow(list, "PHONE PICKUPS", v -> showPickupsInfo());

        LessDivider.addSection(this, list, "BEHAVIOR");
        addRow(list, "BOREDOM MODE", v -> toggleBoredom());
        addRow(list, "SCREEN TIME ALERTS", v -> toggleScreenTimeAlerts());

        LessDivider.addSection(this, list, "APPEARANCE");
        addRow(list, getString(R.string.list_style), v -> cycleListStyle());
        addRow(list, "TEXT SIZE", v -> cycleTextSize());
        addRow(list, "FONT", v -> cycleFont());

        LessDivider.addSection(this, list, "PERMISSIONS");
        addRow(list, "USAGE ACCESS", v ->
                ((LessApplication) getApplication()).getUsageStatsReader().openUsageAccessSettings());
        addRow(list, "ACCESSIBILITY", v -> openAccessibilitySettings());
        addRow(list, "DISPLAY OVER APPS", v ->
                com.ruthless.less.session.SessionTimerService.openOverlaySettings(this));
        addRow(list, "DEFAULT LAUNCHER", v -> openHomeSettings());

        LessDivider.addSection(this, list, "DEVICE");
        addRow(list, getString(R.string.data), v ->
                startActivity(new android.content.Intent(this, DataActivity.class)));
        addRow(list, getString(R.string.about), v ->
                Toast.makeText(this, R.string.about_body, Toast.LENGTH_LONG).show());

        LessDivider.add(this, list);
        addHint(list, "LONG-PRESS AN APP FOR PER-APP ACTIONS");
    }

    private void cycleListStyle() {
        SettingsRepository.ListStyle current = settings.getListStyle();
        SettingsRepository.ListStyle[] values = SettingsRepository.ListStyle.values();
        int next = (current.ordinal() + 1) % values.length;
        settings.setListStyle(values[next]);
        Toast.makeText(this, "LIST STYLE\n" + values[next].name().replace('_', ' '),
                Toast.LENGTH_SHORT).show();
        rebuild();
    }

    private void cycleTextSize() {
        SettingsRepository.TextSize current = settings.getTextSize();
        SettingsRepository.TextSize[] values = SettingsRepository.TextSize.values();
        int next = (current.ordinal() + 1) % values.length;
        settings.setTextSize(values[next]);
        Toast.makeText(this, "TEXT SIZE\n" + values[next].name(), Toast.LENGTH_SHORT).show();
        recreate();
    }

    private void cycleFont() {
        SettingsRepository.FontChoice next =
                settings.getFont() == SettingsRepository.FontChoice.MONOSPACE
                        ? SettingsRepository.FontChoice.SANS
                        : SettingsRepository.FontChoice.MONOSPACE;
        settings.setFont(next);
        Toast.makeText(this, "FONT\n" + next.name(), Toast.LENGTH_SHORT).show();
        recreate();
    }

    private void toggleBoredom() {
        boolean next = !settings.isBoredomModeEnabled();
        settings.setBoredomModeEnabled(next);
        Toast.makeText(this, "BOREDOM MODE\n" + (next ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        rebuild();
    }

    private void toggleScreenTimeAlerts() {
        boolean next = !settings.isScreenTimeAlertsEnabled();
        settings.setScreenTimeAlertsEnabled(next);
        if (next) {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 88);
                }
            }
            com.ruthless.less.usage.ScreenTimeAlertMonitor.get(this).start();
            com.ruthless.less.usage.ScreenTimeAlertMonitor.get(this).checkNow();
        }
        Toast.makeText(this, "SCREEN TIME ALERTS\n" + (next ? "ON\n10 / 20 / 30 MIN…" : "OFF"),
                Toast.LENGTH_SHORT).show();
        rebuild();
    }

    private void showTemperatureInfo() {
        list.removeAllViews();
        addTitle(list, "PHONE TEMPERATURE");
        LessDivider.add(this, list);
        addHint(list, "NOT MEDICAL.");
        addHint(list, "COMPARES TODAY TO YOUR AVERAGE.");
        addHint(list, "COLD COOL NORMAL WARM HOT ON FIRE");
        LessDivider.add(this, list);
        addRow(list, "BACK", v -> rebuild());
    }

    private void showPickupsInfo() {
        LessApplication app = (LessApplication) getApplication();
        list.removeAllViews();
        addTitle(list, "PHONE PICKUPS");
        LessDivider.add(this, list);
        if (!app.getUsageStatsReader().hasUsageAccess()) {
            addHint(list, "USAGE ACCESS REQUIRED");
            LessDivider.add(this, list);
            addRow(list, "OPEN SETTINGS", v -> app.getUsageStatsReader().openUsageAccessSettings());
            addRow(list, "BACK", v -> rebuild());
            return;
        }
        int today = app.getUsageStatsReader().countPickupsForDayOffset(0);
        int yesterday = app.getUsageStatsReader().countPickupsForDayOffset(1);
        int sum = 0;
        int n = 0;
        for (int i = 0; i < 7; i++) {
            int c = app.getUsageStatsReader().countPickupsForDayOffset(i);
            if (c >= 0) {
                sum += c;
                n++;
            }
        }
        addHint(list, "APPROXIMATE UNLOCK / INTERACTION SESSIONS.");
        addHint(list, "OEM REPORTING VARIES.");
        LessDivider.add(this, list);
        addBody(list, "TODAY");
        addBody(list, today < 0 ? "—" : String.valueOf(today));
        addBody(list, "YESTERDAY");
        addBody(list, yesterday < 0 ? "—" : String.valueOf(yesterday));
        addBody(list, "WEEKLY AVG.");
        addBody(list, n == 0 ? "—" : String.valueOf(sum / n));
        LessDivider.add(this, list);
        addRow(list, "BACK", v -> rebuild());
    }

    private void openAccessibilitySettings() {
        Toast.makeText(this, R.string.accessibility_setup_hint, Toast.LENGTH_LONG).show();
        android.content.Intent intent = new android.content.Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void openHomeSettings() {
        android.content.Intent intent = new android.content.Intent(Settings.ACTION_HOME_SETTINGS);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void addTitle(LinearLayout list, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextAppearance(this, R.style.LessText_Title);
        tv.setPadding(0, 0, 0, 8);
        list.addView(tv);
    }

    private void addBody(LinearLayout list, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextAppearance(this, R.style.LessText_Body);
        int pad = getResources().getDimensionPixelSize(R.dimen.less_row_padding_compact);
        tv.setPadding(0, pad, 0, pad);
        list.addView(tv);
    }

    private void addRow(LinearLayout list, String text, View.OnClickListener listener) {
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

    private void addHint(LinearLayout list, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextAppearance(this, R.style.LessText_Hint);
        int pad = getResources().getDimensionPixelSize(R.dimen.less_row_padding_compact);
        tv.setPadding(0, pad, 0, pad);
        list.addView(tv);
    }
}
