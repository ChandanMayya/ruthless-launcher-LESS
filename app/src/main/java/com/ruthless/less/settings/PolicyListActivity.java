package com.ruthless.less.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.ruthless.less.LessApplication;
import com.ruthless.less.R;
import com.ruthless.less.applications.InstalledApp;
import com.ruthless.less.database.PolicyRepository;
import com.ruthless.less.database.entities.AppPolicyEntity;
import com.ruthless.less.launcher.AppMenuActivity;
import com.ruthless.less.launcher.LessActivity;
import com.ruthless.less.ui.LessDivider;

/**
 * Lists apps that have a given policy configured (including uninstalled packages).
 */
public class PolicyListActivity extends LessActivity {

    public static final String EXTRA_FILTER = "filter";

    public static void open(Activity from, PolicyRepository.PolicyFilter filter) {
        Intent intent = new Intent(from, PolicyListActivity.class);
        intent.putExtra(EXTRA_FILTER, filter.name());
        from.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        LinearLayout list = findViewById(R.id.settingsList);

        String filterName = getIntent().getStringExtra(EXTRA_FILTER);
        PolicyRepository.PolicyFilter filter;
        try {
            filter = PolicyRepository.PolicyFilter.valueOf(filterName);
        } catch (Exception e) {
            filter = PolicyRepository.PolicyFilter.LIMITS;
        }

        LessApplication app = (LessApplication) getApplication();
        addTitle(list, titleFor(filter));
        LessDivider.add(this, list);
        addHint(list, "TAP TO EDIT. UNINSTALLED APPS KEEP POLICY.");
        LessDivider.add(this, list);

        final PolicyRepository.PolicyFilter finalFilter = filter;
        app.getPolicyRepository().listFiltered(filter, entities -> runOnUiThread(() -> {
            if (entities.isEmpty()) {
                addBody(list, "NONE CONFIGURED");
                addHint(list, "LONG-PRESS AN APP TO ADD.");
            } else {
                for (AppPolicyEntity entity : entities) {
                    InstalledApp installed = app.getAppRepository().findByPackage(entity.packageName);
                    String label = installed == null
                            ? entity.packageName.toUpperCase()
                            : installed.getDisplayLabel();
                    String status = installed == null ? "UNINSTALLED — POLICY RETAINED" : detail(finalFilter, entity);
                    addRow(list, label, v -> {
                        Intent intent = new Intent(this, AppMenuActivity.class);
                        intent.putExtra(AppMenuActivity.EXTRA_PACKAGE, entity.packageName);
                        intent.putExtra(AppMenuActivity.EXTRA_LABEL, label);
                        startActivity(intent);
                    });
                    addHint(list, status);
                }
            }
            LessDivider.add(this, list);
            addRow(list, getString(R.string.done), v -> finish());
        }));
    }

    private static String titleFor(PolicyRepository.PolicyFilter filter) {
        switch (filter) {
            case CONFUSE:
                return "CONFUSE ME";
            case DELAYS:
                return "LAUNCH DELAYS";
            case CONFIRMATION:
                return "LAUNCH CONFIRMATION";
            case SESSION_TIMER:
                return "SESSION TIMERS";
            case EXCLUSIONS:
                return "SCREEN TIME OFF";
            case LIMITS:
            default:
                return "APP LIMITS";
        }
    }

    private static String detail(PolicyRepository.PolicyFilter filter, AppPolicyEntity e) {
        switch (filter) {
            case CONFUSE:
                return "ON";
            case DELAYS:
                return e.launchDelayMode + " " + (e.launchDelayMinMs / 1000) + "-" + (e.launchDelayMaxMs / 1000) + "S";
            case CONFIRMATION:
                return "ON";
            case SESSION_TIMER:
                return "ASK DURATION ON OPEN";
            case EXCLUSIONS:
                return "MONITORING OFF";
            case LIMITS:
            default:
                return e.dailyLimitMinutes + " MINUTES";
        }
    }

    private void addTitle(LinearLayout list, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextAppearance(this, R.style.LessText_Title);
        tv.setPadding(0, 0, 0, 24);
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

    private void addHint(LinearLayout list, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextAppearance(this, R.style.LessText_Hint);
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
        tv.setOnClickListener(listener);
        list.addView(tv);
    }
}
