package com.ruthless.less.ui;

import android.graphics.Paint;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.UnderlineSpan;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.ruthless.less.R;

/**
 * Marks tappable actions so they stay black/white but read as buttons.
 * Format: [ LABEL ] — brackets plain, label underlined.
 * Home/drawer app names stay plain — only menus, setup, confirmations, settings.
 */
public final class LessActionText {

    private LessActionText() {
    }

    public static void style(@Nullable TextView tv) {
        if (tv == null) {
            return;
        }
        encloseWithUnderlinedLabel(tv);
        // Keep full opacity even if a Hint style was applied earlier.
        tv.setAlpha(1f);
    }

    public static void styleAsAction(TextView tv, android.content.Context context) {
        if (tv == null) {
            return;
        }
        tv.setTextAppearance(context, R.style.LessText_Action);
        style(tv);
    }

    /**
     * Renders {@code [ LABEL ]} with underline only on LABEL.
     * Idempotent if already bracketed.
     */
    static void encloseWithUnderlinedLabel(TextView tv) {
        CharSequence raw = tv.getText();
        if (raw == null) {
            return;
        }
        String text = raw.toString().trim();
        if (text.isEmpty()) {
            return;
        }

        String label = text;
        if (text.startsWith("[") && text.endsWith("]") && text.length() >= 2) {
            label = text.substring(1, text.length() - 1).trim();
        }
        if (label.isEmpty()) {
            return;
        }

        // All-caps transform can refuse length/span changes; labels are already uppercase.
        tv.setAllCaps(false);
        tv.setPaintFlags(tv.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);

        String shown = "[ " + label + " ]";
        SpannableString spanned = new SpannableString(shown);
        int start = 2; // after "[ "
        int end = start + label.length();
        spanned.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(spanned);
    }
}
