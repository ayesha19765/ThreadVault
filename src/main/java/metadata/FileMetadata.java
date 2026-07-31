package metadata;

/**
 * Represents metadata information for a backed-up file.
 *
 * Used by the backup engine for:
 * - incremental backup checks
 * - deduplication
 * - restore operations
 * - metadata persistence
 */
public class FileMetadata {

    private String originalPath;
    private String hash;
    private String backupPath;
    private long originalSize;
    private long compressedSize;
    private String backupTime;
    private long lastModifiedTime;
    private boolean deleted;

    public FileMetadata() {
    }

    public FileMetadata(
            String originalPath,
            String hash,
            String backupPath,
            long originalSize,
            long compressedSize,
            String backupTime,
            long lastModifiedTime,
            boolean deleted
    ) {

        this.originalPath = originalPath;
        this.hash = hash;
        this.backupPath = backupPath;
        this.originalSize = originalSize;
        this.compressedSize = compressedSize;
        this.backupTime = backupTime;
        this.lastModifiedTime = lastModifiedTime;
        this.deleted = deleted;

    }

    public String getOriginalPath() {
        return originalPath;
    }

    public String getHash() {
        return hash;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public long getOriginalSize() {
        return originalSize;
    }

    public long getCompressedSize() {
        return compressedSize;
    }

    public String getBackupTime() {
        return backupTime;
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public String toString() {

        return "FileMetadata{" +
                "originalPath='" + originalPath + '\'' +
                ", hash='" + hash + '\'' +
                ", backupPath='" + backupPath + '\'' +
                ", originalSize=" + originalSize +
                ", compressedSize=" + compressedSize +
                ", backupTime='" + backupTime + '\'' +
                ", lastModifiedTime=" + lastModifiedTime +
                ", deleted=" + deleted +
                '}';

    }

}