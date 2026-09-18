package com.ruthless.less.ui;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.ruthless.less.R;

/**
 * Thin white rules that separate titles, copy, and actions in black/white UI.
 */
public final class LessDivider {

    private LessDivider() {
    }

    /** Hairline rule with vertical breathing room. */
    public static void add(Context context, ViewGroup list) {
        if (context == null || list == null) {
            return;
        }
        View line = new View(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 1));
        int margin = dp(context, 12);
        lp.topMargin = margin;
        lp.bottomMargin = margin;
        line.setLayoutParams(lp);
        line.setBackgroundColor(context.getResources().getColor(R.color.less_white));
        line.setAlpha(0.35f);
        line.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        list.addView(line);
    }

    /**
     * Section label with a rule above it — for grouped lists (settings, pickers).
     */
    public static void addSection(Context context, ViewGroup list, @Nullable String label) {
        add(context, list);
        if (label == null || label.isEmpty()) {
            return;
        }
        TextView tv = new TextView(context);
        tv.setText(label);
        tv.setTextAppearance(context, R.style.LessText_Hint);
        int pad = context.getResources().getDimensionPixelSize(R.dimen.less_row_padding_compact);
        tv.setPadding(0, 0, 0, pad);
        tv.setContentDescription(label);
        list.addView(tv);
    }

    private static int dp(Context context, int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
    }
}
