package com.ruthless.less.settings;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ruthless.less.R;

/**
 * Applies TEXT SIZE / FONT prefs without colorful theming.
 */
public final class UiPreferences {

    private UiPreferences() {
    }

    public static void applyToActivity(Context context, View root, SettingsRepository settings) {
        if (root == null || settings == null) {
            return;
        }
        float scale = settings.textSizeScale();
        Typeface typeface = settings.getFont() == SettingsRepository.FontChoice.SANS
                ? Typeface.SANS_SERIF
                : Typeface.MONOSPACE;
        walk(root, scale, typeface);
    }

    private static void walk(View view, float scale, Typeface typeface) {
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            float base = tv.getTextSize();
            // getTextSize returns px; scale relative to current once per bind via tag guard.
            Object tagged = tv.getTag(R.id.less_ui_scaled);
            if (tagged == null) {
                tv.setTag(R.id.less_ui_scaled, base);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, base * scale);
            }
            tv.setTypeface(typeface);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                walk(group.getChildAt(i), scale, typeface);
            }
        }
    }

    public static Context wrap(Context base, SettingsRepository settings) {
        if (settings == null) {
            return base;
        }
        float scale = settings.textSizeScale();
        if (Math.abs(scale - 1f) < 0.01f) {
            return base;
        }
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.fontScale = config.fontScale * scale;
        return base.createConfigurationContext(config);
    }
}
