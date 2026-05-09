package com.samp.mobile.launcher.ui;

public class DownloadItem {
    public static final int STATUS_IDLE = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_PAUSED = 2;
    public static final int STATUS_COMPLETE = 3;
    public static final int STATUS_FAILED = 4;

    public final String id;
    public final String title;
    public final String description;
    public final String url;
    public final String fileName;
    public final long fileSizeBytes;

    public long downloadManagerId = -1;
    public int status = STATUS_IDLE;
    public long downloadedBytes = 0;
    public long totalBytes = 0;

    public DownloadItem(String id, String title, String description,
                        String url, String fileName, long fileSizeBytes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.url = url;
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
    }

    public int getProgressPercent() {
        if (totalBytes <= 0) return 0;
        return (int)((downloadedBytes * 100L) / totalBytes);
    }

    public boolean isDownloaded() { return status == STATUS_COMPLETE; }
    public boolean isRunning() { return status == STATUS_RUNNING; }
}
