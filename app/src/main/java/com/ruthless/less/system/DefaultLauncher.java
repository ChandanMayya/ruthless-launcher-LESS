package com.ruthless.less.system;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

/**
 * Whether LESS is the current default home app.
 */
public final class DefaultLauncher {

    private DefaultLauncher() {
    }

    public static boolean isLessDefault(Context context) {
        if (context == null) {
            return false;
        }
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo info = context.getPackageManager()
                .resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        return info != null
                && info.activityInfo != null
                && context.getPackageName().equals(info.activityInfo.packageName);
    }
}
