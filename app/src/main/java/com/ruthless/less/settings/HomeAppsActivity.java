package com.ruthless.less.settings;

import android.content.Intent;
import android.os.Bundle;
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

public class HomeAppsActivity extends LessActivity {

    public static final int REQ_PICK = 41;

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
        title.setText(R.string.home_apps);

        RecyclerView list = findViewById(R.id.list);
        adapter = new EditableAppAdapter(new EditableAppAdapter.Callbacks() {
            @Override
            public void onMoveUp(int position) {
                settings.moveHomePackage(position, Math.max(0, position - 1));
                refresh();
            }

            @Override
            public void onMoveDown(int position) {
                List<String> pkgs = settings.getHomePackages();
                settings.moveHomePackage(position, Math.min(pkgs.size() - 1, position + 1));
                refresh();
            }

            @Override
            public void onRemove(int position) {
                List<String> pkgs = settings.getHomePackages();
                if (position >= 0 && position < pkgs.size()) {
                    settings.removeHomePackage(pkgs.get(position));
                    refresh();
                }
            }
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        com.ruthless.less.ui.LessActionText.style(findViewById(R.id.actionAdd));
        com.ruthless.less.ui.LessActionText.style(findViewById(R.id.actionReset));
        com.ruthless.less.ui.LessActionText.style(findViewById(R.id.actionDone));

        findViewById(R.id.actionAdd).setOnClickListener(v -> {
            Intent intent = new Intent(this, AppPickerActivity.class);
            intent.putExtra(AppPickerActivity.EXTRA_MODE, AppPickerActivity.MODE_HOME);
            startActivityForResult(intent, REQ_PICK);
        });
        findViewById(R.id.actionReset).setOnClickListener(v -> {
            settings.resetHomeDefaults(apps);
            refresh();
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
                settings.addHomePackage(pkg);
                refresh();
            }
        }
    }

    private void refresh() {
        List<InstalledApp> resolved = new ArrayList<>();
        for (String pkg : settings.getHomePackages()) {
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
