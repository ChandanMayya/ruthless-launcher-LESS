package com.ruthless.less.launcher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ruthless.less.R;
import com.ruthless.less.applications.InstalledApp;

import java.util.ArrayList;
import java.util.List;

public final class HomeAppsAdapter extends RecyclerView.Adapter<HomeAppsAdapter.Holder> {

    public interface Callbacks {
        void onAppClick(InstalledApp app);

        void onAppLongClick(InstalledApp app);
    }

    private final List<InstalledApp> apps = new ArrayList<>();
    private final Callbacks callbacks;

    public HomeAppsAdapter(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void setApps(List<InstalledApp> next) {
        apps.clear();
        if (next != null) {
            apps.addAll(next);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_row, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        InstalledApp app = apps.get(position);
        holder.label.setText(app.getDisplayLabel());
        holder.label.setContentDescription(app.getDisplayLabel());
        holder.label.setOnClickListener(v -> callbacks.onAppClick(app));
        holder.label.setOnLongClickListener(v -> {
            callbacks.onAppLongClick(app);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView label;

        Holder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.appLabel);
        }
    }
}
