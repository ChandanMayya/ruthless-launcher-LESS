package com.ruthless.less.applications;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

import com.ruthless.less.R;

/**
 * Resolves and launches applications via PackageManager.
 * Never loads icons.
 */
public final class AppLauncher {

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

    /** Opens the system uninstall confirmation for this package. */
    public void uninstall(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return;
        }
        if (packageName.equals(context.getPackageName())) {
            Toast.makeText(context, R.string.cannot_uninstall_self, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.fromParts("package", packageName, null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.cannot_uninstall, Toast.LENGTH_SHORT).show();
        }
    }
}
