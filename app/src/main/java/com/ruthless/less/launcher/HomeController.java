package com.ruthless.less.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Updates clock/date/battery on the home screen.
 * Uses a 30s tick and sticky battery broadcasts — not a tight loop.
 */
public final class HomeController {

    private final Context context;
    private final TextView clockText;
    private final TextView weekdayText;
    private final TextView dateText;
    private final TextView batteryText;
    private final ImageView batteryFlash;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat clockFormat =
            new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat weekdayFormat =
            new SimpleDateFormat("EEEE", Locale.getDefault());
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("d MMMM", Locale.getDefault());

    private BroadcastReceiver batteryReceiver;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateNow();
            handler.postDelayed(this, 30_000L);
        }
    };

    public HomeController(
            Context context,
            TextView clockText,
            TextView weekdayText,
            TextView dateText,
            TextView batteryText,
            ImageView batteryFlash) {
        this.context = context.getApplicationContext();
        this.clockText = clockText;
        this.weekdayText = weekdayText;
        this.dateText = dateText;
        this.batteryText = batteryText;
        this.batteryFlash = batteryFlash;
    }

    public void start() {
        updateNow();
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, 30_000L);
        registerBatteryReceiver();
    }

    public void stop() {
        handler.removeCallbacks(tick);
        unregisterBatteryReceiver();
    }

    public void updateNow() {
        Date now = new Date();
        clockText.setText(clockFormat.format(now));
        weekdayText.setText(weekdayFormat.format(now).toUpperCase(Locale.getDefault()));
        dateText.setText(dateFormat.format(now).toUpperCase(Locale.getDefault()));
        updateBatteryFromSticky();
    }

    private void updateBatteryFromSticky() {
        Intent status = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        applyBatteryIntent(status);
    }

    private void applyBatteryIntent(Intent intent) {
        if (batteryText == null) {
            return;
        }
        if (intent == null) {
            batteryText.setText("—");
            if (batteryFlash != null) {
                batteryFlash.setVisibility(View.GONE);
            }
            return;
        }
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int pct = (level >= 0 && scale > 0) ? Math.round(level * 100f / scale) : -1;

        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL
                || plugged != 0;

        if (pct < 0) {
            batteryText.setText("—");
            if (batteryFlash != null) {
                batteryFlash.setVisibility(View.GONE);
            }
            return;
        }

        batteryText.setText(pct + "%");
        if (batteryFlash != null) {
            batteryFlash.setVisibility(charging ? View.VISIBLE : View.GONE);
        }
        batteryText.setContentDescription(charging
                ? "Battery " + pct + " percent, charging"
                : "Battery " + pct + " percent");
    }

    private void registerBatteryReceiver() {
        if (batteryReceiver != null) {
            return;
        }
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                applyBatteryIntent(intent);
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(batteryReceiver, filter);
        }
    }

    private void unregisterBatteryReceiver() {
        if (batteryReceiver == null) {
            return;
        }
        try {
            context.unregisterReceiver(batteryReceiver);
        } catch (Exception ignored) {
        }
        batteryReceiver = null;
    }
}
