package com.ruthless.less.settings;

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

public final class EditableAppAdapter extends RecyclerView.Adapter<EditableAppAdapter.Holder> {

    public interface Callbacks {
        void onMoveUp(int position);

        void onMoveDown(int position);

        void onRemove(int position);
    }

    private final List<InstalledApp> apps = new ArrayList<>();
    private final Callbacks callbacks;

    public EditableAppAdapter(Callbacks callbacks) {
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
                .inflate(R.layout.item_editable_app, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        InstalledApp app = apps.get(position);
        holder.label.setText(app.getDisplayLabel());
        holder.label.setContentDescription(app.getDisplayLabel());
        com.ruthless.less.ui.LessActionText.style(holder.moveUp);
        com.ruthless.less.ui.LessActionText.style(holder.moveDown);
        com.ruthless.less.ui.LessActionText.style(holder.remove);
        holder.moveUp.setOnClickListener(v -> callbacks.onMoveUp(holder.getBindingAdapterPosition()));
        holder.moveDown.setOnClickListener(v -> callbacks.onMoveDown(holder.getBindingAdapterPosition()));
        holder.remove.setOnClickListener(v -> callbacks.onRemove(holder.getBindingAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView label;
        final TextView moveUp;
        final TextView moveDown;
        final TextView remove;

        Holder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.label);
            moveUp = itemView.findViewById(R.id.moveUp);
            moveDown = itemView.findViewById(R.id.moveDown);
            remove = itemView.findViewById(R.id.remove);
        }
    }
}
