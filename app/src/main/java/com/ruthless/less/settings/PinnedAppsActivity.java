package com.ruthless.less.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ruthless.less.LessApplication;
import com.ruthless.less.R;
import com.ruthless.less.applications.AppRepository;
import com.ruthless.less.applications.InstalledApp;
import com.ruthless.less.launcher.LessActivity;

import java.util.ArrayList;
import java.util.List;

public class PinnedAppsActivity extends LessActivity {

    public static final int REQ_PICK = 42;

    private SettingsRepository settings;
    private AppRepository apps;
    private EditableAppAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_apps);

        LessApplication app = (LessApplication) getApplication();
        settings = app.getSettingsRepository();
        apps = app.getAppRepository();

        TextView title = findViewById(R.id.title);
        title.setText(R.string.pinned_apps);

        // Reset defaults is home-apps only — hide for pins.
        findViewById(R.id.actionReset).setVisibility(View.GONE);

        RecyclerView list = findViewById(R.id.list);
        adapter = new EditableAppAdapter(new EditableAppAdapter.Callbacks() {
            @Override
            public void onMoveUp(int position) {
                settings.movePinned(position, Math.max(0, position - 1));
                refresh();
            }

            @Override
            public void onMoveDown(int position) {
                List<String> pkgs = settings.getPinnedPackages();
                settings.movePinned(position, Math.min(pkgs.size() - 1, position + 1));
                refresh();
            }

            @Override
            public void onRemove(int position) {
                List<String> pkgs = settings.getPinnedPackages();
                if (position >= 0 && position < pkgs.size()) {
                    settings.unpin(pkgs.get(position));
                    refresh();
                }
            }
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        com.ruthless.less.ui.LessActionText.style(findViewById(R.id.actionAdd));
        com.ruthless.less.ui.LessActionText.style(findViewById(R.id.actionDone));

        findViewById(R.id.actionAdd).setOnClickListener(v -> {
            Intent intent = new Intent(this, AppPickerActivity.class);
            intent.putExtra(AppPickerActivity.EXTRA_MODE, AppPickerActivity.MODE_PIN);
            startActivityForResult(intent, REQ_PICK);
        });
        findViewById(R.id.actionDone).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK && resultCode == RESULT_OK && data != null) {
            String pkg = data.getStringExtra(AppPickerActivity.EXTRA_PACKAGE);
            if (pkg != null) {
                settings.pin(pkg);
                refresh();
            }
        }
    }

    private void refresh() {
        List<InstalledApp> resolved = new ArrayList<>();
        for (String pkg : settings.getPinnedPackages()) {
            InstalledApp app = apps.findByPackage(pkg);
            if (app != null) {
                resolved.add(app);
            } else {
                resolved.add(new InstalledApp(pkg, pkg));
            }
        }
        adapter.setApps(resolved);
    }
}
