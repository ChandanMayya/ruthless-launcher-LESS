package com.ruthless.less.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ruthless.less.LessApplication;
import com.ruthless.less.R;
import com.ruthless.less.applications.AppRepository;
import com.ruthless.less.applications.DrawerItem;
import com.ruthless.less.applications.InstalledApp;
import com.ruthless.less.launcher.AppListAdapter;
import com.ruthless.less.launcher.LessActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppPickerActivity extends LessActivity {

    public static final String EXTRA_PACKAGE = "package";
    public static final String EXTRA_MODE = "mode";
    public static final String MODE_HOME = "home";
    public static final String MODE_PIN = "pin";
    public static final String MODE_BOUNDARY = "boundary";

    private AppRepository apps;
    private AppListAdapter adapter;
    private EditText search;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        // Reuse launcher layout but force drawer-only picker UI.
        findViewById(R.id.homePanel).setVisibility(android.view.View.GONE);
        findViewById(R.id.drawerPanel).setVisibility(android.view.View.VISIBLE);
        findViewById(R.id.swipeHint).setVisibility(android.view.View.GONE);
        View chiding = findViewById(R.id.drawerChidingText);
        if (chiding != null) {
            chiding.setVisibility(android.view.View.GONE);
        }

        TextView title = findViewById(R.id.drawerTitle);
        String mode = getIntent().getStringExtra(EXTRA_MODE);
        if (MODE_BOUNDARY.equals(mode)) {
            title.setText("CHOOSE ONE APP");
            title.setContentDescription("Choose one app for a boundary");
        } else {
            title.setText(R.string.add_application);
        }

        apps = ((LessApplication) getApplication()).getAppRepository();
        search = findViewById(R.id.searchInput);
        RecyclerView list = findViewById(R.id.drawerAppsList);

        adapter = new AppListAdapter(new AppListAdapter.Callbacks() {
            @Override
            public void onAppClick(InstalledApp app) {
                Intent data = new Intent();
                data.putExtra(EXTRA_PACKAGE, app.getPackageName());
                setResult(Activity.RESULT_OK, data);
                finish();
            }

            @Override
            public void onAppLongClick(InstalledApp app) {
                onAppClick(app);
            }
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refresh();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        refresh();
    }

    private void refresh() {
        String q = search.getText() == null ? "" : search.getText().toString().trim().toUpperCase(Locale.US);
        List<DrawerItem> items = new ArrayList<>();
        for (InstalledApp app : apps.getLaunchableApps()) {
            if (apps.isConfused(app.getPackageName())) {
                continue;
            }
            if (q.isEmpty()
                    || app.getLabelNormalized().contains(q)
                    || app.getPackageName().toUpperCase(Locale.US).contains(q)) {
                items.add(DrawerItem.app(app));
            }
        }
        if (items.isEmpty()) {
            items.add(DrawerItem.empty(getString(R.string.no_application_found)));
        }
        adapter.setItems(items);
    }
}
