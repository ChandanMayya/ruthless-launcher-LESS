package com.ruthless.less.applications;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.ruthless.less.R;

/**
 * Resolves and launches applications via PackageManager.
 * Never loads icons.
 */
public final class AppLauncher {

    private static final String TAG = "LessUninstall";

    private final Context context;

    public AppLauncher(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean launch(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launch == null) {
            Toast.makeText(context, R.string.cannot_launch, Toast.LENGTH_SHORT).show();
            return false;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(launch);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(context, R.string.cannot_launch, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public void openAppInfo(String packageName) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", packageName, null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.cannot_launch, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Queue uninstall for the foreground launcher activity.
     * Do not start uninstall from a finishing dialog or broadcast — BAL blocks it.
     */
    public void queueUninstallFromMenu(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return;
        }
        if (packageName.equals(context.getPackageName())) {
            Toast.makeText(context, R.string.cannot_uninstall_self, Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(TAG, "queueUninstall " + packageName);
        PendingUninstall.set(packageName);
    }

    /**
     * Must be called from a resumed Activity (typically {@code LauncherActivity}).
     */
    public static boolean startUninstallFromForeground(Activity activity, String packageName) {
        if (activity == null || packageName == null || packageName.isEmpty()) {
            return false;
        }
        if (packageName.equals(activity.getPackageName())) {
            Toast.makeText(activity, R.string.cannot_uninstall_self, Toast.LENGTH_SHORT).show();
            return false;
        }
        Log.i(TAG, "startUninstallFromForeground " + packageName);
        Uri packageUri = Uri.fromParts("package", packageName, null);
        Intent delete = new Intent(Intent.ACTION_DELETE);
        delete.setData(packageUri);
        PackageManager pm = activity.getPackageManager();
        ResolveInfo resolved = pm.resolveActivity(delete, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolved == null) {
            @SuppressWarnings("deprecation")
            Intent uninstall = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
            resolved = pm.resolveActivity(uninstall, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolved == null) {
                try {
                    Intent info = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri);
                    activity.startActivity(info);
                    Toast.makeText(activity, R.string.uninstall_via_app_info, Toast.LENGTH_LONG).show();
                    return true;
                } catch (Exception e) {
                    Toast.makeText(activity, R.string.cannot_uninstall, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            delete = uninstall;
        }
        try {
            activity.startActivity(delete);
            Log.i(TAG, "Uninstaller started from foreground activity");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "foreground uninstall failed", e);
            try {
                Intent info = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri);
                activity.startActivity(info);
                Toast.makeText(activity, R.string.uninstall_via_app_info, Toast.LENGTH_LONG).show();
                return true;
            } catch (Exception e2) {
                Toast.makeText(activity, R.string.cannot_uninstall, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
    }
}
