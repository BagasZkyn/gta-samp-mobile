package com.samp.mobile.launcher.ui;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.samp.mobile.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DownloadCenterFragment extends Fragment {

    /**
     * Game cache files — same source as the old force-download system.
     * Replace these URLs with the actual CDN links from the original download flow.
     * Each entry: { id, title, description, url, filename, sizeBytes }
     */
    private static final Object[][] GAME_FILES = {
            {
                "gta_cache_main",
                "GTA:SA Cache Utama",
                "File cache utama game (textures, models)",
                "https://djava.example.com/cache/gta_main.zip",  // ← ganti URL asli
                "gta_main.zip",
                (long)(800 * 1024 * 1024) // ~800 MB
            },
            {
                "gta_cache_audio",
                "GTA:SA Audio Pack",
                "File audio & soundtrack game",
                "https://djava.example.com/cache/gta_audio.zip", // ← ganti URL asli
                "gta_audio.zip",
                (long)(400 * 1024 * 1024) // ~400 MB
            },
            {
                "samp_cache",
                "SA-MP Resources",
                "File resource SA-MP (script, skin, dll)",
                "https://djava.example.com/cache/samp_res.zip",  // ← ganti URL asli
                "samp_res.zip",
                (long)(50 * 1024 * 1024) // ~50 MB
            },
    };

    private RecyclerView recyclerView;
    private CardView cardOverallProgress;
    private LinearProgressIndicator progressOverall;
    private TextView tvOverallStatus;

    private DownloadManager downloadManager;
    private DownloadAdapter downloadAdapter;
    private List<DownloadItem> downloadItems = new ArrayList<>();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Handler progressHandler;
    private Runnable progressRunnable;

    private final BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long dlId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            for (DownloadItem item : downloadItems) {
                if (item.downloadManagerId == dlId) {
                    item.status = DownloadItem.STATUS_COMPLETE;
                    item.downloadedBytes = item.totalBytes;
                    downloadAdapter.notifyDataSetChanged();
                    updateOverallCard();
                    Toast.makeText(requireContext(), item.title + " selesai didownload!", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_download_center, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_downloads);
        cardOverallProgress = view.findViewById(R.id.card_overall_progress);
        progressOverall = view.findViewById(R.id.progress_overall);
        tvOverallStatus = view.findViewById(R.id.tv_overall_status);

        downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
        buildDownloadItems();

        downloadAdapter = new DownloadAdapter(downloadItems, this::onActionClick, this::onDeleteClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(downloadAdapter);

        restoreDownloadStates();
        startProgressPolling();
    }

    /** Build DownloadItem list from the config array */
    private void buildDownloadItems() {
        downloadItems.clear();
        for (Object[] entry : GAME_FILES) {
            downloadItems.add(new DownloadItem(
                    (String) entry[0],
                    (String) entry[1],
                    (String) entry[2],
                    (String) entry[3],
                    (String) entry[4],
                    (long) entry[5]
            ));
        }
    }

    /** Check if files already downloaded on disk or have active DM entries */
    private void restoreDownloadStates() {
        File dlDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        for (DownloadItem item : downloadItems) {
            if (dlDir != null) {
                File f = new File(dlDir, item.fileName);
                if (f.exists() && f.length() > 0) {
                    item.status = DownloadItem.STATUS_COMPLETE;
                    item.downloadedBytes = f.length();
                    item.totalBytes = f.length();
                }
            }
        }
        downloadAdapter.notifyDataSetChanged();
    }

    private void onActionClick(DownloadItem item) {
        switch (item.status) {
            case DownloadItem.STATUS_IDLE:
            case DownloadItem.STATUS_FAILED:
                startDownload(item);
                break;
            case DownloadItem.STATUS_RUNNING:
                pauseDownload(item);
                break;
            case DownloadItem.STATUS_PAUSED:
                resumeDownload(item);
                break;
            case DownloadItem.STATUS_COMPLETE:
                Toast.makeText(requireContext(), item.title + " sudah didownload!", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void onDeleteClick(DownloadItem item) {
        if (item.downloadManagerId != -1) {
            downloadManager.remove(item.downloadManagerId);
            item.downloadManagerId = -1;
        }
        // Delete file from disk
        File dlDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dlDir != null) {
            new File(dlDir, item.fileName).delete();
        }
        item.status = DownloadItem.STATUS_IDLE;
        item.downloadedBytes = 0;
        item.totalBytes = 0;
        downloadAdapter.notifyDataSetChanged();
        updateOverallCard();
    }

    private void startDownload(DownloadItem item) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(item.url));
        request.setTitle(item.title);
        request.setDescription("Djava Roleplay — " + item.description);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(requireContext(),
                Environment.DIRECTORY_DOWNLOADS, item.fileName);
        request.allowScanningByMediaScanner();

        long dlId = downloadManager.enqueue(request);
        item.downloadManagerId = dlId;
        item.status = DownloadItem.STATUS_RUNNING;
        downloadAdapter.notifyDataSetChanged();
        updateOverallCard();
    }

    private void pauseDownload(DownloadItem item) {
        // DownloadManager doesn't support pause natively — we remove and mark paused
        // Store downloaded bytes first
        if (item.downloadManagerId != -1) {
            updateItemProgress(item);
            downloadManager.remove(item.downloadManagerId);
            item.downloadManagerId = -1;
        }
        item.status = DownloadItem.STATUS_PAUSED;
        downloadAdapter.notifyDataSetChanged();
        updateOverallCard();
    }

    private void resumeDownload(DownloadItem item) {
        // Restart download (DownloadManager doesn't support true resume for arbitrary servers)
        item.status = DownloadItem.STATUS_IDLE;
        startDownload(item);
    }

    /** Poll DownloadManager every second to update progress */
    private void startProgressPolling() {
        progressHandler = new Handler(Looper.getMainLooper());
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                boolean anyRunning = false;
                for (DownloadItem item : downloadItems) {
                    if (item.status == DownloadItem.STATUS_RUNNING && item.downloadManagerId != -1) {
                        updateItemProgress(item);
                        anyRunning = true;
                    }
                }
                if (anyRunning) {
                    downloadAdapter.notifyDataSetChanged();
                    updateOverallCard();
                }
                progressHandler.postDelayed(this, 1000);
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void updateItemProgress(DownloadItem item) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(item.downloadManagerId);
        Cursor cursor = downloadManager.query(query);
        if (cursor != null && cursor.moveToFirst()) {
            int dlIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
            int totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
            int statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            if (dlIdx >= 0) item.downloadedBytes = cursor.getLong(dlIdx);
            if (totalIdx >= 0) item.totalBytes = cursor.getLong(totalIdx);
            if (statusIdx >= 0) {
                int status = cursor.getInt(statusIdx);
                if (status == DownloadManager.STATUS_FAILED) {
                    item.status = DownloadItem.STATUS_FAILED;
                }
            }
            cursor.close();
        }
    }

    private void updateOverallCard() {
        boolean anyRunning = false;
        for (DownloadItem item : downloadItems) {
            if (item.status == DownloadItem.STATUS_RUNNING) { anyRunning = true; break; }
        }
        cardOverallProgress.setVisibility(anyRunning ? View.VISIBLE : View.GONE);
        if (anyRunning) {
            int running = 0;
            for (DownloadItem item : downloadItems) {
                if (item.status == DownloadItem.STATUS_RUNNING) running++;
            }
            tvOverallStatus.setText(running + " file sedang didownload…");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        requireContext().registerReceiver(downloadCompleteReceiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public void onStop() {
        super.onStop();
        try { requireContext().unregisterReceiver(downloadCompleteReceiver); } catch (Exception ignored) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (progressHandler != null && progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }
}
