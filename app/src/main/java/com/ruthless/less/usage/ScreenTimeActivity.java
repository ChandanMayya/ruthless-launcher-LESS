package com.ruthless.less.usage;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.ruthless.less.LessApplication;
import com.ruthless.less.R;
import com.ruthless.less.applications.AppRepository;
import com.ruthless.less.applications.InstalledApp;
import com.ruthless.less.launcher.LessActivity;
import com.ruthless.less.ui.LessDivider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Text-only screen-time dashboard.
 * Label LEFT / value RIGHT on one row. Dividers between sections.
 */
public class ScreenTimeActivity extends LessActivity {

    private enum Period {
        TODAY, WEEK, MONTH, CUSTOM
    }

    private LinearLayout list;
    private Period period = Period.TODAY;
    private int customDays = 14;
    private int renderGeneration;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        list = findViewById(R.id.settingsList);
        LessApplication app = (LessApplication) getApplication();
        if (!app.getSettingsRepository().hasSeenReminder(
                com.ruthless.less.settings.SettingsRepository.REMINDER_SCREEN_TIME)) {
            showScreenTimeReminder();
        } else {
            render();
        }
    }

    private void showScreenTimeReminder() {
        list.removeAllViews();
        addTitle("SCREEN TIME");
        LessDivider.add(this, list);
        addSection("THE PHONE REMEMBERS THE RETURNS.");
        addBody("Totals, pickups, and temperature are receipts — not a scoreboard.");
        LessDivider.add(this, list);
        addAction("CONTINUE", v -> {
            ((LessApplication) getApplication()).getSettingsRepository()
                    .markReminderSeen(com.ruthless.less.settings.SettingsRepository.REMINDER_SCREEN_TIME);
            render();
        });
        addAction("FIELD MANUAL", v -> {
            ((LessApplication) getApplication()).getSettingsRepository()
                    .markReminderSeen(com.ruthless.less.settings.SettingsRepository.REMINDER_SCREEN_TIME);
            com.ruthless.less.tour.FieldManualActivity.open(
                    this, com.ruthless.less.tour.TourCatalog.CHAPTER_EVIDENCE);
            render();
        });
    }

    private void render() {
        final int generation = ++renderGeneration;
        list.removeAllViews();
        LessApplication app = (LessApplication) getApplication();
        UsageStatsReader reader = app.getUsageStatsReader();
        AppRepository apps = app.getAppRepository();

        addTitle("SCREEN TIME");
        addPeriodRow();
        LessDivider.add(this, list);

        if (!reader.hasUsageAccess()) {
            addSection("USAGE ACCESS");
            addBody("REQUIRED FOR SCREEN TIME.");
            addBody("ANDROID SPECIAL ACCESS.");
            LessDivider.add(this, list);
            addAction("OPEN SETTINGS", v -> reader.openUsageAccessSettings());
            LessDivider.add(this, list);
            addAction("BACK", v -> finish());
            return;
        }

        if (period == Period.TODAY) {
            final AppRepository appsRef = apps;
            reader.queryTodayAsync(true, (rows, totalMs) -> runOnUiThread(() -> {
                if (generation != renderGeneration) {
                    return;
                }
                renderTodayContent(appsRef, rows, totalMs);
                LessDivider.add(this, list);
                addAction("BACK", v -> finish());
            }));
            return;
        }

        int days = period == Period.WEEK ? 7 : (period == Period.MONTH ? 30 : customDays);
        String heading = period == Period.WEEK ? "THIS WEEK"
                : (period == Period.MONTH ? "THIS MONTH" : "LAST " + customDays + " DAYS");
        addSection(heading);
        app.getUsageRepository().loadRangeSummary(days, summary -> runOnUiThread(() -> {
            if (generation != renderGeneration) {
                return;
            }
            for (UsageRepository.DayTotal day : summary.days) {
                addKv(formatDayLabel(day.date), UsageStatsReader.formatMinutes(day.totalMinutes));
            }
            LessDivider.add(this, list);
            addSection("SUMMARY");
            addKv("AVERAGE", UsageStatsReader.formatMinutes(summary.averageMinutes) + " / DAY");
            String delta = summary.deltaMinutes >= 0
                    ? "+" + UsageStatsReader.formatMinutes(summary.deltaMinutes)
                    : "-" + UsageStatsReader.formatMinutes(Math.abs(summary.deltaMinutes));
            addKv("VS PREVIOUS", delta + " / DAY");
            addKv("PREVIOUS AVG",
                    UsageStatsReader.formatMinutes(summary.previousAverageMinutes) + " / DAY");
            LessDivider.add(this, list);
            addAction("BACK", v -> finish());
        }));
    }

    private void addPeriodRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = getResources().getDimensionPixelSize(R.dimen.less_row_padding_compact);
        row.setPadding(0, pad, 0, pad);

        addPeriodChip(row, "TODAY", Period.TODAY, false);
        addPeriodSep(row);
        addPeriodChip(row, "WEEK", Period.WEEK, false);
        addPeriodSep(row);
        addPeriodChip(row, "MONTH", Period.MONTH, false);
        addPeriodSep(row);
        String customLabel = period == Period.CUSTOM ? customDays + "D" : "CUSTOM";
        addPeriodChip(row, customLabel, Period.CUSTOM, true);
        list.addView(row);
    }

    private void addPeriodSep(LinearLayout row) {
        TextView sep = new TextView(this);
        sep.setText("  |  ");
        sep.setTextAppearance(this, R.style.LessText_Hint);
        row.addView(sep);
    }

    private void addPeriodChip(LinearLayout row, String label, Period target, boolean customCycle) {
        boolean selected = period == target;
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextAppearance(this, selected ? R.style.LessText_Body : R.style.LessText_Hint);
        if (!selected) {
            tv.setAlpha(0.7f);
        }
        com.ruthless.less.ui.LessActionText.style(tv);
        tv.setClickable(true);
        tv.setFocusable(true);
        tv.setContentDescription(label);
        tv.setOnClickListener(v -> {
            if (customCycle && period == Period.CUSTOM) {
                customDays = customDays == 14 ? 21 : (customDays == 21 ? 10 : 14);
            }
            period = target;
            render();
        });
        row.addView(tv);
    }

    private void renderTodayContent(AppRepository apps, List<UsageStatsReader.UsageRow> rows,
                                    long total) {
        addSection("TODAY");
        addKv("TOTAL", UsageStatsReader.formatDuration(total));

        LessApplication app = (LessApplication) getApplication();
        int pickups = app.getUsageStatsReader().countPickupsForDayOffset(0);
        addKv("PICKUPS", pickups < 0 ? "—" : String.valueOf(pickups));
        LessDivider.add(this, list);

        addSection("APPS");
        List<UsageStatsReader.UsageRow> visible = new ArrayList<>();
        long accounted = 0;
        for (UsageStatsReader.UsageRow row : rows) {
            if (row.totalTimeMs < 60_000L) {
                continue;
            }
            if (shouldHidePackage(row.packageName)) {
                continue;
            }
            String label = labelFor(apps, row.packageName);
            if (label == null) {
                continue;
            }
            visible.add(row);
        }

        int shown = 0;
        for (UsageStatsReader.UsageRow row : visible) {
            if (shown >= 15) {
                break;
            }
            String label = labelFor(apps, row.packageName);
            addKv(label, UsageStatsReader.formatDuration(row.totalTimeMs));
            accounted += row.totalTimeMs;
            shown++;
        }

        long other = Math.max(0, total - accounted);
        if (other >= 60_000L) {
            addKv("OTHER", UsageStatsReader.formatDuration(other));
        }

        if (shown == 0) {
            addBody("NO APP USAGE YET TODAY.");
        }
    }

    /**
     * Prefer launchable app labels. Resolve other packages via PackageManager.
     * Returns null to skip noisy system components.
     */
    @Nullable
    private String labelFor(AppRepository apps, String packageName) {
        if (packageName == null || packageName.equals(getPackageName())) {
            return null;
        }
        InstalledApp installed = apps.findByPackage(packageName);
        if (installed != null) {
            return installed.getDisplayLabel();
        }
        if (shouldHidePackage(packageName)) {
            return null;
        }
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            CharSequence label = pm.getApplicationLabel(info);
            if (label == null) {
                return null;
            }
            String text = label.toString().trim();
            if (text.isEmpty() || looksLikePackageName(text)) {
                return null;
            }
            return text.toUpperCase(Locale.US);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean looksLikePackageName(String text) {
        return text.contains(".") && text.equals(text.toLowerCase(Locale.US));
    }

    private static boolean shouldHidePackage(String packageName) {
        String p = packageName.toLowerCase(Locale.US);
        return p.contains("launcher")
                || p.contains("packageinstaller")
                || p.contains("systemui")
                || p.contains("accessibility")
                || p.contains("permissioncontroller")
                || p.contains("settings.intelligence")
                || p.endsWith(".cts")
                || p.contains("com.android.shell")
                || p.contains("com.google.android.gms")
                || p.contains("com.google.android.gsf")
                || p.contains("com.samsung.android.app.cocktailbarservice")
                || p.contains("com.sec.android.app.launcher");
    }

    private static String formatDayLabel(String yyyyMmDd) {
        if (yyyyMmDd == null || yyyyMmDd.length() < 10) {
            return yyyyMmDd;
        }
        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("EEE d MMM", Locale.US);
            return out.format(in.parse(yyyyMmDd)).toUpperCase(Locale.US);
        } catch (Exception e) {
            return yyyyMmDd;
        }
    }

    private void addTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextAppearance(this, R.style.LessText_Title);
        tv.setPadding(0, 0, 0, 8);
        list.addView(tv);
    }

    private void addSection(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextAppearance(this, R.style.LessText_Hint);
        int pad = getResources().getDimensionPixelSize(R.dimen.less_row_padding_compact);
        tv.setPadding(0, pad, 0, pad);
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

    private void addKv(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = getResources().getDimensionPixelSize(R.dimen.less_row_padding_normal);
        row.setPadding(0, pad / 2, 0, pad / 2);

        TextView left = new TextView(this);
        left.setText(label);
        left.setTextAppearance(this, R.style.LessText_Body);
        left.setMaxLines(1);
        left.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        left.setLayoutParams(leftLp);

        TextView right = new TextView(this);
        right.setText(value);
        right.setTextAppearance(this, R.style.LessText_Body);
        right.setGravity(Gravity.END);
        right.setPadding(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12,
                        getResources().getDisplayMetrics()),
                0, 0, 0);

        row.addView(left);
        row.addView(right);
        row.setContentDescription(label + " " + value);
        list.addView(row);
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
        list.addView(tv);
    }
}
