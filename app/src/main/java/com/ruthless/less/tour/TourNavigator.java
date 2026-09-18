package com.ruthless.less.tour;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;

import com.ruthless.less.LessApplication;
import com.ruthless.less.database.PolicyRepository;
import com.ruthless.less.session.SessionTimerService;
import com.ruthless.less.settings.DataActivity;
import com.ruthless.less.settings.HomeAppsActivity;
import com.ruthless.less.settings.PinnedAppsActivity;
import com.ruthless.less.settings.PolicyListActivity;
import com.ruthless.less.usage.ScreenTimeActivity;

/**
 * Shared deep-links from tour / Field Manual actions.
 */
public final class TourNavigator {

    private TourNavigator() {
    }

    /**
     * @return true if the action was handled here (caller need not do more)
     */
    public static boolean navigate(Activity activity, TourAction action, TourBeat beat) {
        if (activity == null || action == null) {
            return false;
        }
        LessApplication app = (LessApplication) activity.getApplication();
        switch (action) {
            case OPEN_HOME_SETTINGS:
                activity.startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                return true;
            case OPEN_USAGE_ACCESS:
                app.getUsageStatsReader().openUsageAccessSettings();
                return true;
            case OPEN_ACCESSIBILITY:
                activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                return true;
            case OPEN_OVERLAY_SETTINGS:
                SessionTimerService.openOverlaySettings(activity);
                return true;
            case OPEN_HOME_APPS:
                activity.startActivity(new Intent(activity, HomeAppsActivity.class));
                return true;
            case OPEN_PINNED_APPS:
                activity.startActivity(new Intent(activity, PinnedAppsActivity.class));
                return true;
            case OPEN_SCREEN_TIME:
                activity.startActivity(new Intent(activity, ScreenTimeActivity.class));
                return true;
            case OPEN_DATA:
                activity.startActivity(new Intent(activity, DataActivity.class));
                return true;
            case OPEN_POLICY_LIMITS:
                PolicyListActivity.open(activity, PolicyRepository.PolicyFilter.LIMITS);
                return true;
            case OPEN_POLICY_CONFUSE:
                PolicyListActivity.open(activity, PolicyRepository.PolicyFilter.CONFUSE);
                return true;
            case OPEN_POLICY_DELAYS:
                PolicyListActivity.open(activity, PolicyRepository.PolicyFilter.DELAYS);
                return true;
            case OPEN_POLICY_CONFIRMATION:
                PolicyListActivity.open(activity, PolicyRepository.PolicyFilter.CONFIRMATION);
                return true;
            case OPEN_POLICY_SESSION_TIMER:
                PolicyListActivity.open(activity, PolicyRepository.PolicyFilter.SESSION_TIMER);
                return true;
            case OPEN_POLICY_EXCLUSIONS:
                PolicyListActivity.open(activity, PolicyRepository.PolicyFilter.EXCLUSIONS);
                return true;
            case OPEN_FIELD_MANUAL:
                FieldManualActivity.open(activity, null);
                return true;
            case OPEN_FIELD_CHAPTER:
                FieldManualActivity.open(activity, beat != null ? beat.chapterId : null);
                return true;
            default:
                return false;
        }
    }
}
