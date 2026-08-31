package model;

import stats.BackupStatistics;

import java.time.LocalDateTime;

/**
 * Thread-safe domain model representing a backup job and its execution state.
 */
public class BackupJob {

    private final String id;
    private final String sourcePath;
    private final String destinationPath;
    private final int workerCount;
    private final LocalDateTime createdAt;

    private volatile BackupStatus status;
    private volatile LocalDateTime startedAt;
    private volatile LocalDateTime completedAt;
    private volatile String errorMessage;
    private final BackupStatistics statistics;

    public BackupJob(String id, String sourcePath, String destinationPath, int workerCount) {
        this.id = id;
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
        this.workerCount = workerCount;
        this.status = BackupStatus.QUEUED;
        this.createdAt = LocalDateTime.now();
        this.statistics = new BackupStatistics();
    }

    public String getId() {
        return id;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public BackupStatus getStatus() {
        return status;
    }

    public void setStatus(BackupStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public BackupStatistics getStatistics() {
        return statistics;
    }
}
