package service;

import dto.response.CatalogFileResponse;
import dto.response.CatalogPageResponse;
import dto.response.CatalogSummaryResponse;
import metadata.FileMetadata;
import metadata.MetadataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of CatalogService querying the metadata repository.
 */
@Service
public class CatalogServiceImpl implements CatalogService {

    private final MetadataStore metadataStore;
    private final BackupJobRegistry jobRegistry;

    @Autowired
    public CatalogServiceImpl(BackupJobRegistry jobRegistry) {
        this(new MetadataStore(), jobRegistry);
    }

    public CatalogServiceImpl(MetadataStore metadataStore, BackupJobRegistry jobRegistry) {
        this.metadataStore = metadataStore != null ? metadataStore : new MetadataStore();
        this.jobRegistry = jobRegistry;
    }

    @Override
    public CatalogSummaryResponse getSummary() {
        List<FileMetadata> all = metadataStore.getAllMetadata();

        int totalFiles = all.size();
        Set<String> uniqueHashes = all.stream()
                .map(FileMetadata::getHash)
                .filter(h -> h != null && !h.isBlank())
                .collect(Collectors.toSet());

        int uniqueFiles = uniqueHashes.size();
        long totalOriginalBytes = all.stream()
                .mapToLong(FileMetadata::getOriginalSize)
                .sum();

        // Stored bytes represent sum of unique archives stored in backup_storage
        long totalStoredBytes = all.stream()
                .filter(m -> m.getHash() != null)
                .collect(Collectors.toMap(FileMetadata::getHash, FileMetadata::getCompressedSize, (a, b) -> a))
                .values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        long deduplicatedBytes = Math.max(0L, totalOriginalBytes - totalStoredBytes);
        double spaceSavedPercentage = 0.0;
        if (totalOriginalBytes > 0) {
            double compPct = (totalStoredBytes * 100.0) / totalOriginalBytes;
            spaceSavedPercentage = Math.max(0.0, Math.round((100.0 - compPct) * 100.0) / 100.0);
        }

        long distinctBackupTimestamps = all.stream()
                .map(FileMetadata::getBackupTime)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .count();

        int totalBackups = (int) Math.max(jobRegistry != null ? jobRegistry.count() : 0, distinctBackupTimestamps);

        String lastBackupTime = all.stream()
                .map(FileMetadata::getBackupTime)
                .filter(t -> t != null && !t.isBlank())
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new CatalogSummaryResponse(
                totalFiles,
                uniqueFiles,
                totalOriginalBytes,
                totalStoredBytes,
                deduplicatedBytes,
                spaceSavedPercentage,
                totalBackups,
                lastBackupTime
        );
    }

    @Override
    public CatalogPageResponse<CatalogFileResponse> getFiles(String pathFilter, String hashFilter, int page, int size) {
        List<FileMetadata> all = metadataStore.getAllMetadata();

        // Count occurrences per hash to identify deduplicated files
        Map<String, Long> hashCounts = all.stream()
                .filter(m -> m.getHash() != null)
                .collect(Collectors.groupingBy(FileMetadata::getHash, Collectors.counting()));

        // Apply filters
        List<CatalogFileResponse> filtered = all.stream()
                .filter(m -> {
                    if (pathFilter != null && !pathFilter.isBlank()) {
                        return m.getOriginalPath() != null &&
                                m.getOriginalPath().toLowerCase().contains(pathFilter.toLowerCase().trim());
                    }
                    return true;
                })
                .filter(m -> {
                    if (hashFilter != null && !hashFilter.isBlank()) {
                        return m.getHash() != null &&
                                m.getHash().toLowerCase().contains(hashFilter.toLowerCase().trim());
                    }
                    return true;
                })
                .sorted(Comparator.comparing(FileMetadata::getOriginalPath, Comparator.nullsLast(String::compareTo)))
                .map(m -> {
                    boolean isDedup = hashCounts.getOrDefault(m.getHash(), 1L) > 1L;
                    return CatalogFileResponse.from(m, isDedup);
                })
                .toList();

        int clampedPage = Math.max(0, page);
        int clampedSize = Math.max(1, Math.min(size > 0 ? size : 20, 100));
        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / clampedSize);

        int fromIndex = clampedPage * clampedSize;
        List<CatalogFileResponse> pageContent;
        if (fromIndex >= totalElements) {
            pageContent = List.of();
        } else {
            int toIndex = Math.min(fromIndex + clampedSize, totalElements);
            pageContent = filtered.subList(fromIndex, toIndex);
        }

        boolean hasMore = (clampedPage + 1) * clampedSize < totalElements;

        return new CatalogPageResponse<>(
                pageContent,
                clampedPage,
                clampedSize,
                totalElements,
                totalPages,
                hasMore
        );
    }
}

