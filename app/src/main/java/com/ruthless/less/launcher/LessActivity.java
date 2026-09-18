package com.ruthless.less.launcher;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ruthless.less.LessApplication;
import com.ruthless.less.settings.SettingsRepository;
import com.ruthless.less.settings.UiPreferences;

/**
 * Applies TEXT SIZE / FONT prefs to every LESS screen.
 */
public abstract class LessActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        SettingsRepository settings = null;
        try {
            Context appCtx = newBase.getApplicationContext();
            if (appCtx instanceof LessApplication) {
                settings = ((LessApplication) appCtx).getSettingsRepository();
            }
        } catch (Exception ignored) {
        }
        super.attachBaseContext(UiPreferences.wrap(newBase, settings));
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        LessApplication app = (LessApplication) getApplication();
        UiPreferences.applyToActivity(this, findViewById(android.R.id.content),
                app.getSettingsRepository());
    }
}
