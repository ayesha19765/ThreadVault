package metadata;

public class FileMetadata {

    private String originalPath;
    private String hash;
    private String backupPath;
    private long originalSize;
    private long compressedSize;
    private String backupTime;

    public FileMetadata() {
    }

    public FileMetadata(
            String originalPath,
            String hash,
            String backupPath,
            long originalSize,
            long compressedSize,
            String backupTime) {

        this.originalPath = originalPath;
        this.hash = hash;
        this.backupPath = backupPath;
        this.originalSize = originalSize;
        this.compressedSize = compressedSize;
        this.backupTime = backupTime;
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
}