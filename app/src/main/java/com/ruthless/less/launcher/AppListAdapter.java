package com.ruthless.less.launcher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ruthless.less.R;
import com.ruthless.less.applications.DrawerItem;
import com.ruthless.less.applications.InstalledApp;
import com.ruthless.less.settings.SettingsRepository;

import java.util.ArrayList;
import java.util.List;

public final class AppListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Callbacks {
        void onAppClick(InstalledApp app);

        void onAppLongClick(InstalledApp app);
    }

    private static final int TYPE_SECTION = 0;
    private static final int TYPE_APP = 1;
    private static final int TYPE_EMPTY = 2;

    private final List<DrawerItem> items = new ArrayList<>();
    private final Callbacks callbacks;
    private SettingsRepository.ListStyle listStyle = SettingsRepository.ListStyle.NORMAL;

    public AppListAdapter(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void setItems(List<DrawerItem> next) {
        items.clear();
        if (next != null) {
            items.addAll(next);
        }
        notifyDataSetChanged();
    }

    public void setListStyle(SettingsRepository.ListStyle style) {
        this.listStyle = style == null ? SettingsRepository.ListStyle.NORMAL : style;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        switch (items.get(position).type) {
            case SECTION:
                return TYPE_SECTION;
            case EMPTY:
                return TYPE_EMPTY;
            case APP:
            default:
                return TYPE_APP;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SECTION || viewType == TYPE_EMPTY) {
            View view = inflater.inflate(R.layout.item_section_header, parent, false);
            return new TextHolder(view);
        }
        View view = inflater.inflate(R.layout.item_app_row, parent, false);
        return new AppHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DrawerItem item = items.get(position);
        if (holder instanceof AppHolder) {
            AppHolder appHolder = (AppHolder) holder;
            appHolder.label.setText(item.text);
            applyPadding(appHolder.label);
            appHolder.label.setContentDescription(item.text);
            appHolder.label.setOnClickListener(v -> {
                if (item.app != null) {
                    callbacks.onAppClick(item.app);
                }
            });
            appHolder.label.setOnLongClickListener(v -> {
                if (item.app != null) {
                    callbacks.onAppLongClick(item.app);
                }
                return true;
            });
        } else if (holder instanceof TextHolder) {
            TextHolder textHolder = (TextHolder) holder;
            textHolder.label.setText(item.text);
            textHolder.label.setContentDescription(item.text);
        }
    }

    private void applyPadding(TextView view) {
        int pad;
        switch (listStyle) {
            case COMPACT:
                pad = view.getResources().getDimensionPixelSize(R.dimen.less_row_padding_compact);
                break;
            case SPACED:
                pad = view.getResources().getDimensionPixelSize(R.dimen.less_row_padding_spaced);
                break;
            case VERY_SPACED:
                pad = view.getResources().getDimensionPixelSize(R.dimen.less_row_padding_very_spaced);
                break;
            case NORMAL:
            default:
                pad = view.getResources().getDimensionPixelSize(R.dimen.less_row_padding_normal);
                break;
        }
        view.setPadding(view.getPaddingLeft(), pad, view.getPaddingRight(), pad);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class AppHolder extends RecyclerView.ViewHolder {
        final TextView label;

        AppHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.appLabel);
        }
    }

    static final class TextHolder extends RecyclerView.ViewHolder {
        final TextView label;

        TextHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.sectionLabel);
        }
    }
}
