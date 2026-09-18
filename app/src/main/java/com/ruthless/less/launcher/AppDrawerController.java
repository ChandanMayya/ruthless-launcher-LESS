package com.ruthless.less.launcher;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ruthless.less.applications.AppRepository;
import com.ruthless.less.applications.InstalledApp;
import com.ruthless.less.boredom.BoredomMessageManager;
import com.ruthless.less.confuse.ConfuseManager;
import com.ruthless.less.settings.SettingsRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Owns drawer visibility, search filtering, and list binding.
 */
public final class AppDrawerController {

    public interface Host {
        void onAppClick(InstalledApp app);

        void onAppLongClick(InstalledApp app);
    }

    private final View drawerPanel;
    private final View homePanel;
    private final EditText searchInput;
    private final TextView drawerChidingText;
    private final AppRepository appRepository;
    private final SettingsRepository settings;
    private final ConfuseManager confuseManager;
    private final BoredomMessageManager boredom;
    private final AppListAdapter adapter;
    private final RecyclerView drawerList;
    private boolean open;

    public AppDrawerController(
            View homePanel,
            View drawerPanel,
            EditText searchInput,
            TextView drawerChidingText,
            RecyclerView drawerList,
            AppRepository appRepository,
            SettingsRepository settings,
            ConfuseManager confuseManager,
            BoredomMessageManager boredom,
            Host host) {
        this.homePanel = homePanel;
        this.drawerPanel = drawerPanel;
        this.searchInput = searchInput;
        this.drawerChidingText = drawerChidingText;
        this.drawerList = drawerList;
        this.appRepository = appRepository;
        this.settings = settings;
        this.confuseManager = confuseManager;
        this.boredom = boredom;
        this.adapter = new AppListAdapter(new AppListAdapter.Callbacks() {
            @Override
            public void onAppClick(InstalledApp app) {
                host.onAppClick(app);
            }

            @Override
            public void onAppLongClick(InstalledApp app) {
                host.onAppLongClick(app);
            }
        });
        drawerList.setLayoutManager(new LinearLayoutManager(drawerList.getContext()));
        drawerList.setAdapter(adapter);
        confuseManager.getExposureTracker().setListener(packageName ->
                drawerList.post(this::refresh));
        drawerList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@androidx.annotation.NonNull RecyclerView recyclerView, int dx, int dy) {
                confuseManager.getExposureTracker().onScrolled(dy);
            }
        });
        searchInput.addTextChangedListener(new TextWatcher() {
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
    }

    public boolean isOpen() {
        return open;
    }

    public void open() {
        open = true;
        searchInput.setText("");
        adapter.setListStyle(settings.getListStyle());
        showDrawerChiding();
        startConfuseSessionThenRefresh();
        drawerPanel.setVisibility(View.VISIBLE);
        homePanel.setVisibility(View.GONE);
        searchInput.requestFocus();
    }

    public void close() {
        open = false;
        drawerPanel.setVisibility(View.GONE);
        homePanel.setVisibility(View.VISIBLE);
        searchInput.clearFocus();
    }

    public void refresh() {
        adapter.setListStyle(settings.getListStyle());
        String query = searchInput.getText() == null ? "" : searchInput.getText().toString();
        adapter.setItems(appRepository.buildDrawerItems(query));
    }

    private void showDrawerChiding() {
        if (drawerChidingText == null) {
            return;
        }
        if (!settings.isBoredomModeEnabled()) {
            drawerChidingText.setText("");
            drawerChidingText.setVisibility(View.GONE);
            return;
        }
        drawerChidingText.setVisibility(View.VISIBLE);
        drawerChidingText.setText(boredom.nextDrawerChide());
    }

    private void startConfuseSessionThenRefresh() {
        List<String> pinned = settings.getPinnedPackages();
        Set<String> pinnedSet = new HashSet<>(pinned);
        List<String> normals = new ArrayList<>();
        for (InstalledApp app : appRepository.getLaunchableApps()) {
            if (!pinnedSet.contains(app.getPackageName())) {
                normals.add(app.getPackageName());
            }
        }
        confuseManager.startSessionAsync(normals, () -> drawerList.post(this::refresh));
        refresh();
    }
}
