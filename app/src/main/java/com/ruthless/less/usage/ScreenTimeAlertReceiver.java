package com.ruthless.less.usage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Alarm tick for overall screen-time milestone checks.
 */
public class ScreenTimeAlertReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (ScreenTimeAlertMonitor.checkAction().equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            ScreenTimeAlertMonitor.get(context).start();
            ScreenTimeAlertMonitor.get(context).checkNow();
        }
    }
}
