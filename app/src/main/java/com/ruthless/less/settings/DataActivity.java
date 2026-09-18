package com.ruthless.less.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.ruthless.less.LessApplication;
import com.ruthless.less.R;
import com.ruthless.less.launcher.LessActivity;
import com.ruthless.less.ui.LessDivider;

/**
 * Local data management — no cloud, no export servers.
 */
public class DataActivity extends LessActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        LinearLayout list = findViewById(R.id.settingsList);
        LessApplication app = (LessApplication) getApplication();

        addTitle(list, getString(R.string.data));
        LessDivider.add(this, list);
        addHint(list, "EVERYTHING STAYS ON THIS DEVICE.");
        addHint(list, "NO CLOUD. NO ACCOUNT.");
        LessDivider.add(this, list);

        addRow(list, "CLEAR USAGE HISTORY", v ->
                app.getPolicyRepository().clearFrictionsAndHistoryAsync(() ->
                        runOnUiThread(() -> {
                            app.getSettingsRepository().clearInteractionHistory();
                            Toast.makeText(this, "USAGE HISTORY CLEARED", Toast.LENGTH_SHORT).show();
                        })));

        addRow(list, "CLEAR ALL APP POLICIES", v ->
                app.getPolicyRepository().clearAllPoliciesAsync(() ->
                        runOnUiThread(() ->
                                Toast.makeText(this, "POLICIES CLEARED", Toast.LENGTH_SHORT).show())));

        addRow(list, "RESET ONBOARDING", v -> {
            app.getSettingsRepository().setOnboardingDone(false);
            Toast.makeText(this, "ONBOARDING WILL SHOW NEXT LAUNCH", Toast.LENGTH_SHORT).show();
        });

        LessDivider.add(this, list);
        addRow(list, getString(R.string.done), v -> finish());
    }

    private void addTitle(LinearLayout list, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextAppearance(this, R.style.LessText_Title);
        tv.setPadding(0, 0, 0, 24);
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
