package com.samp.mobile.launcher.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.samp.mobile.R;

import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.ViewHolder> {

    public interface ActionListener {
        void onAction(DownloadItem item);
    }

    public interface DeleteListener {
        void onDelete(DownloadItem item);
    }

    private final List<DownloadItem> items;
    private final ActionListener actionListener;
    private final DeleteListener deleteListener;

    public DownloadAdapter(List<DownloadItem> items,
                           ActionListener actionListener,
                           DeleteListener deleteListener) {
        this.items = items;
        this.actionListener = actionListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_download, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        DownloadItem item = items.get(position);
        h.tvTitle.setText(item.title);
        h.tvDesc.setText(item.description);

        // Size label
        h.tvSize.setText(formatSize(item.downloadedBytes) + " / " + formatSize(item.fileSizeBytes));

        // Progress
        boolean showProgress = item.status == DownloadItem.STATUS_RUNNING
                || item.status == DownloadItem.STATUS_PAUSED;
        h.progressBar.setVisibility(showProgress ? View.VISIBLE : View.GONE);
        if (showProgress) {
            h.progressBar.setIndeterminate(item.totalBytes <= 0);
            if (item.totalBytes > 0) {
                h.progressBar.setProgressCompat(item.getProgressPercent(), true);
            }
        }

        // Status chip
        String statusLabel;
        int statusBgColor;
        switch (item.status) {
            case DownloadItem.STATUS_RUNNING:
                statusLabel = h.itemView.getContext().getString(R.string.download_status_running);
                statusBgColor = R.color.md_theme_dark_primaryContainer;
                break;
            case DownloadItem.STATUS_PAUSED:
                statusLabel = h.itemView.getContext().getString(R.string.download_status_paused);
                statusBgColor = R.color.md_theme_dark_tertiaryContainer;
                break;
            case DownloadItem.STATUS_COMPLETE:
                statusLabel = h.itemView.getContext().getString(R.string.download_status_complete);
                statusBgColor = R.color.md_theme_dark_secondaryContainer;
                break;
            case DownloadItem.STATUS_FAILED:
                statusLabel = h.itemView.getContext().getString(R.string.download_status_failed);
                statusBgColor = R.color.md_theme_dark_surfaceVariant;
                break;
            default:
                statusLabel = h.itemView.getContext().getString(R.string.download_status_pending);
                statusBgColor = R.color.md_theme_dark_surfaceVariant;
                break;
        }
        h.chipStatus.setText(statusLabel);
        h.chipStatus.setChipBackgroundColorResource(statusBgColor);

        // Action button label
        String actionLabel;
        switch (item.status) {
            case DownloadItem.STATUS_RUNNING:
                actionLabel = h.itemView.getContext().getString(R.string.download_btn_pause);
                break;
            case DownloadItem.STATUS_PAUSED:
                actionLabel = h.itemView.getContext().getString(R.string.download_btn_resume);
                break;
            case DownloadItem.STATUS_COMPLETE:
                actionLabel = "✅ Selesai";
                break;
            default:
                actionLabel = h.itemView.getContext().getString(R.string.download_btn_start);
                break;
        }
        h.btnAction.setText(actionLabel);
        h.btnAction.setOnClickListener(v -> actionListener.onAction(item));

        // Delete button — visible only when paused or complete
        boolean showDelete = item.status == DownloadItem.STATUS_COMPLETE
                || item.status == DownloadItem.STATUS_PAUSED
                || item.status == DownloadItem.STATUS_FAILED;
        h.btnDelete.setVisibility(showDelete ? View.VISIBLE : View.GONE);
        h.btnDelete.setOnClickListener(v -> deleteListener.onDelete(item));
    }

    @Override
    public int getItemCount() { return items.size(); }

    private String formatSize(long bytes) {
        if (bytes <= 0) return "0 MB";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvSize;
        Chip chipStatus;
        LinearProgressIndicator progressBar;
        MaterialButton btnAction, btnDelete;

        ViewHolder(@NonNull View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tv_dl_title);
            tvDesc = view.findViewById(R.id.tv_dl_desc);
            tvSize = view.findViewById(R.id.tv_dl_size);
            chipStatus = view.findViewById(R.id.chip_dl_status);
            progressBar = view.findViewById(R.id.progress_dl);
            btnAction = view.findViewById(R.id.btn_dl_action);
            btnDelete = view.findViewById(R.id.btn_dl_delete);
        }
    }
}
