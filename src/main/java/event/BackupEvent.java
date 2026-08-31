package event;

import java.time.LocalDateTime;

/**
 * Domain event payload representing a discrete backup progress or lifecycle event.
 */
public class BackupEvent {

    private final String backupId;
    private final BackupEventType type;
    private final LocalDateTime timestamp;
    private final String file;
    private final Long fileSize;
    private final int filesDiscovered;
    private final int filesProcessed;
    private final int filesSkipped;
    private final int filesDeduplicated;
    private final int filesIncrementalSkipped;
    private final int filesFailed;
    private final long storedBytes;
    private final double spaceSavedPercentage;
    private final String message;

    private BackupEvent(Builder builder) {
        this.backupId = builder.backupId;
        this.type = builder.type;
        this.timestamp = builder.timestamp != null ? builder.timestamp : LocalDateTime.now();
        this.file = builder.file;
        this.fileSize = builder.fileSize;
        this.filesDiscovered = builder.filesDiscovered;
        this.filesProcessed = builder.filesProcessed;
        this.filesSkipped = builder.filesSkipped;
        this.filesDeduplicated = builder.filesDeduplicated;
        this.filesIncrementalSkipped = builder.filesIncrementalSkipped;
        this.filesFailed = builder.filesFailed;
        this.storedBytes = builder.storedBytes;
        this.spaceSavedPercentage = builder.spaceSavedPercentage;
        this.message = builder.message;
    }

    public static Builder builder(String backupId, BackupEventType type) {
        return new Builder(backupId, type);
    }

    public String getBackupId() {
        return backupId;
    }

    public BackupEventType getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFile() {
        return file;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public int getFilesDiscovered() {
        return filesDiscovered;
    }

    public int getFilesProcessed() {
        return filesProcessed;
    }

    public int getFilesSkipped() {
        return filesSkipped;
    }

    public int getFilesDeduplicated() {
        return filesDeduplicated;
    }

    public int getFilesIncrementalSkipped() {
        return filesIncrementalSkipped;
    }

    public int getFilesFailed() {
        return filesFailed;
    }

    public long getStoredBytes() {
        return storedBytes;
    }

    public double getSpaceSavedPercentage() {
        return spaceSavedPercentage;
    }

    public String getMessage() {
        return message;
    }

    public static class Builder {
        private final String backupId;
        private final BackupEventType type;
        private LocalDateTime timestamp = LocalDateTime.now();
        private String file;
        private Long fileSize;
        private int filesDiscovered;
        private int filesProcessed;
        private int filesSkipped;
        private int filesDeduplicated;
        private int filesIncrementalSkipped;
        private int filesFailed;
        private long storedBytes;
        private double spaceSavedPercentage;
        private String message;

        public Builder(String backupId, BackupEventType type) {
            this.backupId = backupId;
            this.type = type;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder file(String file) {
            this.file = file;
            return this;
        }

        public Builder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public Builder stats(
                int discovered,
                int processed,
                int incrementalSkipped,
                int deduplicated,
                int failed,
                long stored,
                double spaceSaved
        ) {
            this.filesDiscovered = discovered;
            this.filesProcessed = processed;
            this.filesIncrementalSkipped = incrementalSkipped;
            this.filesDeduplicated = deduplicated;
            this.filesSkipped = incrementalSkipped + deduplicated;
            this.filesFailed = failed;
            this.storedBytes = stored;
            this.spaceSavedPercentage = spaceSaved;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public BackupEvent build() {
            return new BackupEvent(this);
        }
    }
}

