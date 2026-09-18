package com.ruthless.less.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ruthless.less.LessApplication;
import com.ruthless.less.R;
import com.ruthless.less.accessibility.UsageEnforcementAccessibilityService;
import com.ruthless.less.applications.InstalledApp;
import com.ruthless.less.database.PolicyRepository;
import com.ruthless.less.launcher.LessActivity;
import com.ruthless.less.session.SessionTimerService;
import com.ruthless.less.tour.TourAction;
import com.ruthless.less.tour.TourBeat;
import com.ruthless.less.tour.TourCatalog;
import com.ruthless.less.tour.TourCheck;
import com.ruthless.less.tour.TourNavigator;
import com.ruthless.less.tour.TourPageRenderer;
import com.ruthless.less.ui.LessDivider;

import java.util.List;

/**
 * First-run “Future You” tour — narrative, short, optional boundary.
 */
public class OnboardingActivity extends LessActivity implements TourPageRenderer.ActionHandler {

    private static final int REQ_PICK_BOUNDARY = 61;
    private static final int REQ_NOTIFICATIONS = 62;

    private LinearLayout list;
    private TextView stepLabel;
    private List<TourBeat> beats;
    private int index;
    @Nullable
    private String boundaryPackage;
    @Nullable
    private String boundaryLabel;
    private enum BoundaryPhase { NONE, CHOOSE_KIND }
    private BoundaryPhase boundaryPhase = BoundaryPhase.NONE;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        list = findViewById(R.id.onboardingList);
        stepLabel = findViewById(R.id.onboardingStep);
        beats = TourCatalog.firstRunBeats();
        SettingsRepository settings = ((LessApplication) getApplication()).getSettingsRepository();
        int saved = settings.getOnboardingStep();
        index = Math.min(saved, Math.max(0, beats.size() - 1));
        showCurrent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh permission status lines when returning from system settings.
        if (index >= 0 && index < beats.size() && !beats.get(index).boundaryStep) {
            showCurrent();
        }
    }

    private void showCurrent() {
        if (index < 0) {
            index = 0;
        }
        if (index >= beats.size()) {
            finishOnboarding();
            return;
        }
        ((LessApplication) getApplication()).getSettingsRepository().setOnboardingStep(index);
        TourBeat beat = beats.get(index);

        boolean stepComplete = false;
        String statusLine = null;
        if (beat.check == TourCheck.DEFAULT_HOME) {
            stepComplete = com.ruthless.less.system.DefaultLauncher.isLessDefault(this);
            statusLine = stepComplete
                    ? "DONE — LESS IS YOUR DEFAULT HOME"
                    : "NOT YET — SET LESS AS DEFAULT LAUNCHER";
        } else if (beat.check == TourCheck.USAGE_ACCESS) {
            stepComplete = ((LessApplication) getApplication()).getUsageStatsReader().hasUsageAccess();
            statusLine = stepComplete
                    ? "DONE — USAGE ACCESS GRANTED"
                    : "NOT YET — USAGE ACCESS NOT GRANTED";
        } else if (beat.check == TourCheck.ACCESSIBILITY) {
            stepComplete = UsageEnforcementAccessibilityService.isConnected();
            statusLine = stepComplete
                    ? "DONE — ACCESSIBILITY ON (LEAVE SHORTCUT OFF)"
                    : "NOT YET — TURN LESS ON IN ACCESSIBILITY";
        } else if (beat.check == TourCheck.OVERLAY) {
            stepComplete = SessionTimerService.canDrawOverlays(this);
            statusLine = stepComplete
                    ? "DONE — DISPLAY OVER APPS ALLOWED"
                    : "NOT YET — ALLOW DISPLAY OVER APPS";
        } else if (beat.check == TourCheck.NOTIFICATIONS) {
            stepComplete = hasNotificationPermission();
            statusLine = stepComplete
                    ? "DONE — NOTIFICATIONS ALLOWED"
                    : "NOT YET — ALLOW NOTIFICATIONS";
        }

        if (stepComplete) {
            stepLabel.setText((index + 1) + " / " + beats.size() + "  ·  DONE");
            stepLabel.setContentDescription("Tour step " + (index + 1) + " of " + beats.size() + ", completed");
        } else {
            stepLabel.setText((index + 1) + " / " + beats.size());
            stepLabel.setContentDescription("Tour step " + (index + 1) + " of " + beats.size());
        }

        if (beat.boundaryStep) {
            renderBoundaryStep(beat);
            return;
        }

        TourPageRenderer.render(this, list, beat, statusLine, stepComplete, null, this);
    }

    private void renderBoundaryStep(TourBeat beat) {
        list.removeAllViews();
        if (beat.section != null) {
            TourPageRenderer.addHint(this, list, beat.section);
        }
        TourPageRenderer.addTitle(this, list, beat.title);
        TourPageRenderer.addLead(this, list, beat.lead);
        if (beat.demoSlotId != null) {
            TourPageRenderer.addDemoSlot(this, list, beat.demoSlotId);
        }
        for (String paragraph : beat.body) {
            TourPageRenderer.addBody(this, list, paragraph);
        }
        if (beat.warning != null) {
            TourPageRenderer.addHint(this, list, beat.warning);
        }

        LessDivider.add(this, list);

        if (boundaryPackage != null && boundaryPhase == BoundaryPhase.CHOOSE_KIND) {
            TourPageRenderer.addHint(this, list, "SELECTED: " + (boundaryLabel != null
                    ? boundaryLabel : boundaryPackage));
            TourPageRenderer.addAction(this, list, "LAUNCH CONFIRMATION", v ->
                    applyBoundary(true));
            TourPageRenderer.addAction(this, list, "LAUNCH DELAY 0–10 SEC", v ->
                    applyBoundary(false));
            TourPageRenderer.addAction(this, list, "CHOOSE A DIFFERENT APP", v -> {
                boundaryPackage = null;
                boundaryLabel = null;
                boundaryPhase = BoundaryPhase.NONE;
                renderBoundaryStep(beat);
            });
        } else {
            TourPageRenderer.addAction(this, list, "CHOOSE ONE APP", v ->
                    onTourAction(TourAction.START_BOUNDARY_PICK, beat));
        }

        TourPageRenderer.addAction(this, list, "BEGIN WITH NO BOUNDARY YET", v ->
                onTourAction(TourAction.SKIP_BOUNDARY, beat));
    }

    private void applyBoundary(boolean confirmation) {
        if (boundaryPackage == null) {
            return;
        }
        PolicyRepository policies = ((LessApplication) getApplication()).getPolicyRepository();
        Runnable done = () -> runOnUiThread(() -> {
            Toast.makeText(this,
                    confirmation ? "CONFIRMATION ON" : "DELAY 0–10 SEC ON",
                    Toast.LENGTH_SHORT).show();
            goNext();
        });
        if (confirmation) {
            policies.setLaunchConfirmation(boundaryPackage, true, done);
        } else {
            policies.setLaunchDelayRange(boundaryPackage, "RANGE", 0, 10_000, done);
        }
    }

    @Override
    public void onTourAction(TourAction action, TourBeat beat) {
        if (action == TourAction.OPEN_ACCESSIBILITY) {
            Toast.makeText(this, R.string.accessibility_setup_hint, Toast.LENGTH_LONG).show();
        }
        if (action == TourAction.REQUEST_NOTIFICATIONS) {
            requestNotificationPermission();
            return;
        }
        if (TourNavigator.navigate(this, action, beat)) {
            return;
        }
        switch (action) {
            case NEXT:
                goNext();
                break;
            case FINISH_ONBOARDING:
                finishOnboarding();
                break;
            case START_BOUNDARY_PICK:
                Intent pick = new Intent(this, AppPickerActivity.class);
                pick.putExtra(AppPickerActivity.EXTRA_MODE, AppPickerActivity.MODE_BOUNDARY);
                startActivityForResult(pick, REQ_PICK_BOUNDARY);
                break;
            case SKIP_BOUNDARY:
                boundaryPackage = null;
                boundaryLabel = null;
                boundaryPhase = BoundaryPhase.NONE;
                goNext();
                break;
            case BOUNDARY_CONFIRMATION:
                applyBoundary(true);
                break;
            case BOUNDARY_DELAY:
                applyBoundary(false);
                break;
            default:
                break;
        }
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) {
            return true;
        }
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNotificationPermission() {
        if (hasNotificationPermission()) {
            Toast.makeText(this, "DONE — NOTIFICATIONS ALLOWED", Toast.LENGTH_SHORT).show();
            showCurrent();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATIONS) {
            showCurrent();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Become-default-home can recreate the tour activity; restore persisted step.
        SettingsRepository settings = ((LessApplication) getApplication()).getSettingsRepository();
        if (!settings.isOnboardingDone() && beats != null && !beats.isEmpty()) {
            int saved = settings.getOnboardingStep();
            index = Math.min(saved, beats.size() - 1);
            boundaryPhase = BoundaryPhase.NONE;
            showCurrent();
        }
    }

    private void goNext() {
        index++;
        boundaryPhase = BoundaryPhase.NONE;
        showCurrent();
    }

    private void finishOnboarding() {
        ((LessApplication) getApplication()).getSettingsRepository().setOnboardingDone(true);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_BOUNDARY || resultCode != Activity.RESULT_OK || data == null) {
            return;
        }
        String pkg = data.getStringExtra(AppPickerActivity.EXTRA_PACKAGE);
        if (pkg == null) {
            return;
        }
        boundaryPackage = pkg;
        boundaryLabel = resolveLabel(pkg);
        boundaryPhase = BoundaryPhase.CHOOSE_KIND;
        if (index < beats.size()) {
            renderBoundaryStep(beats.get(index));
        }
    }

    private String resolveLabel(String pkg) {
        for (InstalledApp app : ((LessApplication) getApplication()).getAppRepository().getLaunchableApps()) {
            if (pkg.equals(app.getPackageName())) {
                return app.getDisplayLabel();
            }
        }
        return pkg.toUpperCase();
    }

    @Override
    public void onBackPressed() {
        if (index > 0) {
            index--;
            boundaryPhase = BoundaryPhase.NONE;
            showCurrent();
            return;
        }
        // Stay on first beat — completion is intentional.
    }
}
