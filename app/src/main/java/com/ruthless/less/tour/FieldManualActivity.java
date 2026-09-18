package com.ruthless.less.tour;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.ruthless.less.R;
import com.ruthless.less.launcher.LessActivity;
import com.ruthless.less.ui.LessDivider;

/**
 * Revisitable Field Manual — full feature chapters, not a one-shot walkthrough.
 */
public class FieldManualActivity extends LessActivity implements TourPageRenderer.ActionHandler {

    public static final String EXTRA_CHAPTER = "chapter_id";

    private LinearLayout list;
    private TextView stepLabel;
    @Nullable
    private String chapterId;

    public static void open(Activity from, @Nullable String chapterId) {
        Intent intent = new Intent(from, FieldManualActivity.class);
        if (chapterId != null) {
            intent.putExtra(EXTRA_CHAPTER, chapterId);
        }
        from.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        list = findViewById(R.id.onboardingList);
        stepLabel = findViewById(R.id.onboardingStep);
        chapterId = getIntent().getStringExtra(EXTRA_CHAPTER);
        render();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        chapterId = intent != null ? intent.getStringExtra(EXTRA_CHAPTER) : null;
        render();
    }

    private void render() {
        if (chapterId == null || chapterId.isEmpty()) {
            renderIndex();
            return;
        }
        TourBeat chapter = TourCatalog.fieldManualChapter(chapterId);
        if (chapter == null) {
            chapterId = null;
            renderIndex();
            return;
        }
        stepLabel.setText("FIELD MANUAL");
        stepLabel.setContentDescription("Field Manual chapter");
        TourPageRenderer.render(this, list, chapter, null, null, this);
        LessDivider.add(this, list);
        TourPageRenderer.addAction(this, list, "BACK TO INDEX", v -> {
            chapterId = null;
            render();
        });
    }

    private void renderIndex() {
        stepLabel.setText("FIELD MANUAL");
        stepLabel.setContentDescription("Field Manual index");
        list.removeAllViews();
        TourPageRenderer.addTitle(this, list, "FIELD MANUAL");
        TourPageRenderer.addLead(this, list,
                "A map of the boundaries. Read when calm. Return when the urge returns.");
        TourPageRenderer.addBody(this, list,
                "Each chapter names a future regret, what LESS changes, and the honest trade-off.");
        LessDivider.add(this, list);
        for (TourBeat entry : TourCatalog.fieldManualIndex()) {
            final String id = entry.chapterId;
            TourPageRenderer.addAction(this, list, entry.title, v -> {
                chapterId = id;
                render();
            });
        }
        LessDivider.add(this, list);
        TourPageRenderer.addAction(this, list, "CLOSE", v -> finish());
    }

    @Override
    public void onTourAction(TourAction action, TourBeat beat) {
        if (action == TourAction.OPEN_FIELD_CHAPTER && beat != null && beat.chapterId != null) {
            chapterId = beat.chapterId;
            render();
            return;
        }
        if (action == TourAction.DISMISS) {
            finish();
            return;
        }
        TourNavigator.navigate(this, action, beat);
    }

    @Override
    public void onBackPressed() {
        if (chapterId != null) {
            chapterId = null;
            render();
            return;
        }
        super.onBackPressed();
    }
}
