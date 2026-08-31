package dto.response;

import model.BackupJob;
import model.BackupStatus;
import stats.BackupStatistics;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Response DTO representing backup job details, execution duration, and statistics.
 */
public class BackupJobResponse {

    private String backupId;
    private BackupStatus status;
    private String source;
    private String destination;
    private int workers;
    private int filesDiscovered;
    private int filesProcessed;
    private int filesSkipped;
    private int filesDeduplicated;
    private int filesIncrementalSkipped;
    private int filesFailed;
    private long originalBytes;
    private long storedBytes;
    private double spaceSavedPercentage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationMs;
    private String errorMessage;

    public BackupJobResponse() {
    }

    public static BackupJobResponse from(BackupJob job) {
        BackupJobResponse response = new BackupJobResponse();
        response.setBackupId(job.getId());
        response.setStatus(job.getStatus());
        response.setSource(job.getSourcePath());
        response.setDestination(job.getDestinationPath());
        response.setWorkers(job.getWorkerCount());
        response.setCreatedAt(job.getCreatedAt());
        response.setStartedAt(job.getStartedAt());
        response.setCompletedAt(job.getCompletedAt());
        response.setErrorMessage(job.getErrorMessage());

        if (job.getStartedAt() != null) {
            LocalDateTime end = job.getCompletedAt() != null ? job.getCompletedAt() : LocalDateTime.now();
            response.setDurationMs(Duration.between(job.getStartedAt(), end).toMillis());
        } else {
            response.setDurationMs(0L);
        }

        BackupStatistics stats = job.getStatistics();
        if (stats != null) {
            response.setFilesDiscovered(stats.getFilesScanned());
            response.setFilesProcessed(stats.getFilesBackedUp());
            response.setFilesDeduplicated(stats.getDuplicatesSkipped());
            response.setFilesIncrementalSkipped(stats.getIncrementalSkipped());
            response.setFilesSkipped(stats.getTotalSkipped());
            response.setFilesFailed(stats.getFailedFiles());
            response.setOriginalBytes(stats.getOriginalBytes());
            response.setStoredBytes(stats.getCompressedBytes());

            if (stats.getOriginalBytes() > 0) {
                double compressionPct = (stats.getCompressedBytes() * 100.0) / stats.getOriginalBytes();
                response.setSpaceSavedPercentage(Math.max(0.0, Math.round((100.0 - compressionPct) * 100.0) / 100.0));
            } else {
                response.setSpaceSavedPercentage(0.0);
            }
        }

        return response;
    }

    public String getBackupId() {
        return backupId;
    }

    public void setBackupId(String backupId) {
        this.backupId = backupId;
    }

    public BackupStatus getStatus() {
        return status;
    }

    public void setStatus(BackupStatus status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public int getWorkers() {
        return workers;
    }

    public void setWorkers(int workers) {
        this.workers = workers;
    }

    public int getFilesDiscovered() {
        return filesDiscovered;
    }

    public void setFilesDiscovered(int filesDiscovered) {
        this.filesDiscovered = filesDiscovered;
    }

    public int getFilesProcessed() {
        return filesProcessed;
    }

    public void setFilesProcessed(int filesProcessed) {
        this.filesProcessed = filesProcessed;
    }

    public int getFilesSkipped() {
        return filesSkipped;
    }

    public void setFilesSkipped(int filesSkipped) {
        this.filesSkipped = filesSkipped;
    }

    public int getFilesDeduplicated() {
        return filesDeduplicated;
    }

    public void setFilesDeduplicated(int filesDeduplicated) {
        this.filesDeduplicated = filesDeduplicated;
    }

    public int getFilesIncrementalSkipped() {
        return filesIncrementalSkipped;
    }

    public void setFilesIncrementalSkipped(int filesIncrementalSkipped) {
        this.filesIncrementalSkipped = filesIncrementalSkipped;
    }

    public int getFilesFailed() {
        return filesFailed;
    }

    public void setFilesFailed(int filesFailed) {
        this.filesFailed = filesFailed;
    }

    public long getOriginalBytes() {
        return originalBytes;
    }

    public void setOriginalBytes(long originalBytes) {
        this.originalBytes = originalBytes;
    }

    public long getStoredBytes() {
        return storedBytes;
    }

    public void setStoredBytes(long storedBytes) {
        this.storedBytes = storedBytes;
    }

    public double getSpaceSavedPercentage() {
        return spaceSavedPercentage;
    }

    public void setSpaceSavedPercentage(double spaceSavedPercentage) {
        this.spaceSavedPercentage = spaceSavedPercentage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
