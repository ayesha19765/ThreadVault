package model;

/**
 * Represents the lifecycle status of a backup job.
 */
public enum BackupStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
