package com.ruthless.less.applications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ruthless.less.launcher.LauncherActivity;

/**
 * Test/helper: queues uninstall and brings the launcher to the foreground so BAL allows it.
 * adb shell am broadcast -n com.ruthless.less/.applications.UninstallRequestReceiver
 *   -a com.ruthless.less.action.REQUEST_UNINSTALL --es package &lt;pkg&gt;
 */
public final class UninstallRequestReceiver extends BroadcastReceiver {

    public static final String ACTION = "com.ruthless.less.action.REQUEST_UNINSTALL";
    public static final String EXTRA_PACKAGE = "package";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction())) {
            return;
        }
        String pkg = intent.getStringExtra(EXTRA_PACKAGE);
        if (pkg == null || pkg.isEmpty()) {
            return;
        }
        PendingUninstall.set(pkg);
        Intent home = new Intent(context, LauncherActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        home.putExtra(LauncherActivity.EXTRA_UNINSTALL_PACKAGE, pkg);
        context.startActivity(home);
    }
}
