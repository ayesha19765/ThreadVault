package event;

/**
 * Enumeration of domain event types emitted during backup lifecycle and file processing.
 */
public enum BackupEventType {
    BACKUP_STARTED,
    FILE_DISCOVERED,
    FILE_PROCESSED,
    FILE_SKIPPED,
    FILE_DEDUPLICATED,
    FILE_FAILED,
    BACKUP_COMPLETED,
    BACKUP_FAILED
}

