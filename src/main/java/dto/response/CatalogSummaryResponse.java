package dto.response;

/**
 * Summary statistics of the backup catalog and deduplication repository.
 */
public class CatalogSummaryResponse {

    private int totalFiles;
    private int uniqueFiles;
    private long totalOriginalBytes;
    private long totalStoredBytes;
    private long deduplicatedBytes;
    private double spaceSavedPercentage;
    private int totalBackups;
    private String lastBackupTime;

    public CatalogSummaryResponse() {
    }

    public CatalogSummaryResponse(
            int totalFiles,
            int uniqueFiles,
            long totalOriginalBytes,
            long totalStoredBytes,
            long deduplicatedBytes,
            double spaceSavedPercentage,
            int totalBackups,
            String lastBackupTime
    ) {
        this.totalFiles = totalFiles;
        this.uniqueFiles = uniqueFiles;
        this.totalOriginalBytes = totalOriginalBytes;
        this.totalStoredBytes = totalStoredBytes;
        this.deduplicatedBytes = deduplicatedBytes;
        this.spaceSavedPercentage = spaceSavedPercentage;
        this.totalBackups = totalBackups;
        this.lastBackupTime = lastBackupTime;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getUniqueFiles() {
        return uniqueFiles;
    }

    public void setUniqueFiles(int uniqueFiles) {
        this.uniqueFiles = uniqueFiles;
    }

    public long getTotalOriginalBytes() {
        return totalOriginalBytes;
    }

    public void setTotalOriginalBytes(long totalOriginalBytes) {
        this.totalOriginalBytes = totalOriginalBytes;
    }

    public long getTotalStoredBytes() {
        return totalStoredBytes;
    }

    public void setTotalStoredBytes(long totalStoredBytes) {
        this.totalStoredBytes = totalStoredBytes;
    }

    public long getDeduplicatedBytes() {
        return deduplicatedBytes;
    }

    public void setDeduplicatedBytes(long deduplicatedBytes) {
        this.deduplicatedBytes = deduplicatedBytes;
    }

    public double getSpaceSavedPercentage() {
        return spaceSavedPercentage;
    }

    public void setSpaceSavedPercentage(double spaceSavedPercentage) {
        this.spaceSavedPercentage = spaceSavedPercentage;
    }

    public int getTotalBackups() {
        return totalBackups;
    }

    public void setTotalBackups(int totalBackups) {
        this.totalBackups = totalBackups;
    }

    public String getLastBackupTime() {
        return lastBackupTime;
    }

    public void setLastBackupTime(String lastBackupTime) {
        this.lastBackupTime = lastBackupTime;
    }
}

