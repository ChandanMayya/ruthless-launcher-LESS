package com.ruthless.less.tour;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.ruthless.less.R;
import com.ruthless.less.ui.LessActionText;
import com.ruthless.less.ui.LessDivider;

/**
 * Renders a {@link TourBeat} into a vertical list using LESS text styles.
 */
public final class TourPageRenderer {

    public interface ActionHandler {
        void onTourAction(TourAction action, TourBeat beat);
    }

    private TourPageRenderer() {
    }

    public static void render(
            Context context,
            LinearLayout list,
            TourBeat beat,
            @Nullable String statusLine,
            boolean stepComplete,
            @Nullable View extraBlock,
            ActionHandler handler) {
        list.removeAllViews();
        if (beat == null) {
            return;
        }

        if (beat.section != null && !beat.section.isEmpty()) {
            addHint(context, list, beat.section);
        }

        addTitle(context, list, beat.title);
        addLead(context, list, beat.lead);

        if (statusLine != null && !statusLine.isEmpty()) {
            addStatus(context, list, stepComplete, statusLine);
        }

        if (beat.demoSlotId != null) {
            addDemoSlot(context, list, beat.demoSlotId);
        }

        for (String paragraph : beat.body) {
            if (paragraph != null && !paragraph.isEmpty()) {
                addBody(context, list, paragraph);
            }
        }

        if (beat.warning != null && !beat.warning.isEmpty()) {
            addHint(context, list, beat.warning);
        }

        if (extraBlock != null) {
            list.addView(extraBlock);
        }

        LessDivider.add(context, list);

        if (stepComplete && beat.secondaryAction == TourAction.NEXT) {
            // Prefer continuing when the check already passed.
            addAction(context, list, "CONTINUE — STEP DONE", v ->
                    handler.onTourAction(TourAction.NEXT, beat));
            if (beat.primaryLabel != null && beat.primaryAction != null
                    && beat.primaryAction != TourAction.NEXT) {
                addAction(context, list, beat.primaryLabel, v ->
                        handler.onTourAction(beat.primaryAction, beat));
            }
            return;
        }

        if (beat.primaryLabel != null && beat.primaryAction != null) {
            addAction(context, list, beat.primaryLabel, v ->
                    handler.onTourAction(beat.primaryAction, beat));
        }
        if (beat.secondaryLabel != null && beat.secondaryAction != null) {
            String secondary = beat.secondaryLabel;
            if (stepComplete && beat.secondaryAction == TourAction.NEXT) {
                secondary = "CONTINUE — STEP DONE";
            }
            addAction(context, list, secondary, v ->
                    handler.onTourAction(beat.secondaryAction, beat));
        }
    }

    /** @deprecated use {@link #render(Context, LinearLayout, TourBeat, String, boolean, View, ActionHandler)} */
    public static void render(
            Context context,
            LinearLayout list,
            TourBeat beat,
            @Nullable String usageStatusLine,
            @Nullable View extraBlock,
            ActionHandler handler) {
        boolean done = usageStatusLine != null && usageStatusLine.contains("DONE");
        render(context, list, beat, usageStatusLine, done, extraBlock, handler);
    }

    public static void addStatus(Context context, LinearLayout list, boolean done, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextAppearance(context, R.style.LessText_Body);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setAllCaps(true);
        tv.setLetterSpacing(0.08f);
        tv.setPadding(0, dp(context, 8), 0, dp(context, 16));
        tv.setContentDescription(text);
        tv.setAlpha(done ? 1f : 0.75f);
        list.addView(tv);
        LessDivider.add(context, list);
    }

    public static void addAction(Context context, LinearLayout list, String text, View.OnClickListener listener) {
        TextView tv = new TextView(context);
        tv.setText(text);
        LessActionText.styleAsAction(tv, context);
        tv.setPadding(0, dp(context, 14), 0, dp(context, 14));
        tv.setClickable(true);
        tv.setFocusable(true);
        tv.setContentDescription(text);
        tv.setOnClickListener(listener);
        list.addView(tv);
    }

    public static void addTitle(Context context, LinearLayout list, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextAppearance(context, R.style.LessText_Title);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        tv.setLetterSpacing(0.12f);
        tv.setPadding(0, 0, 0, dp(context, 8));
        tv.setContentDescription(text);
        list.addView(tv);
    }

    public static void addLead(Context context, LinearLayout list, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextAppearance(context, R.style.LessText_Body);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setAllCaps(false);
        tv.setAlpha(0.92f);
        tv.setLineSpacing(dp(context, 3), 1f);
        tv.setPadding(0, 0, 0, dp(context, 14));
        tv.setContentDescription(text);
        list.addView(tv);
        LessDivider.add(context, list);
    }

    public static void addBody(Context context, LinearLayout list, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextAppearance(context, R.style.LessText_Body);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setAllCaps(false);
        tv.setLineSpacing(dp(context, 4), 1f);
        tv.setPadding(0, 0, 0, dp(context, 12));
        tv.setContentDescription(text);
        list.addView(tv);
    }

    public static void addHint(Context context, LinearLayout list, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextAppearance(context, R.style.LessText_Hint);
        tv.setAllCaps(false);
        tv.setPadding(0, dp(context, 4), 0, dp(context, 12));
        tv.setContentDescription(text);
        list.addView(tv);
    }

    /**
     * Empty reserved region for a future local silent monochrome demonstration.
     * No media, no network — layout slot only.
     */
    public static void addDemoSlot(Context context, LinearLayout list, String slotId) {
        FrameLayout slot = new FrameLayout(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 4));
        lp.bottomMargin = dp(context, 12);
        slot.setLayoutParams(lp);
        slot.setContentDescription("Demonstration reserved: " + slotId);
        slot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        list.addView(slot);
    }

    private static int dp(Context context, int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
    }
}
