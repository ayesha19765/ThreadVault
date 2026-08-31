package dto.response;

import metadata.FileMetadata;

/**
 * Detailed representation of a cataloged backed-up file entry.
 */
public class CatalogFileResponse {

    private String originalPath;
    private String hash;
    private String backupPath;
    private long originalSize;
    private long compressedSize;
    private String backupTime;
    private long lastModifiedTime;
    private boolean deleted;
    private boolean deduplicated;

    public CatalogFileResponse() {
    }

    public static CatalogFileResponse from(FileMetadata metadata, boolean isDeduplicated) {
        CatalogFileResponse response = new CatalogFileResponse();
        response.setOriginalPath(metadata.getOriginalPath());
        response.setHash(metadata.getHash());
        response.setBackupPath(metadata.getBackupPath());
        response.setOriginalSize(metadata.getOriginalSize());
        response.setCompressedSize(metadata.getCompressedSize());
        response.setBackupTime(metadata.getBackupTime());
        response.setLastModifiedTime(metadata.getLastModifiedTime());
        response.setDeleted(metadata.isDeleted());
        response.setDeduplicated(isDeduplicated);
        return response;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }

    public long getOriginalSize() {
        return originalSize;
    }

    public void setOriginalSize(long originalSize) {
        this.originalSize = originalSize;
    }

    public long getCompressedSize() {
        return compressedSize;
    }

    public void setCompressedSize(long compressedSize) {
        this.compressedSize = compressedSize;
    }

    public String getBackupTime() {
        return backupTime;
    }

    public void setBackupTime(String backupTime) {
        this.backupTime = backupTime;
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeduplicated() {
        return deduplicated;
    }

    public void setDeduplicated(boolean deduplicated) {
        this.deduplicated = deduplicated;
    }
}

