package service;

import dto.response.CatalogFileResponse;
import dto.response.CatalogPageResponse;
import dto.response.CatalogSummaryResponse;
import metadata.FileMetadata;
import metadata.MetadataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CatalogServiceTest {

    private BackupJobRegistry jobRegistry;

    @BeforeEach
    void setUp() {
        jobRegistry = new BackupJobRegistry();
    }

    private static class MockMetadataStore extends MetadataStore {
        private final List<FileMetadata> list;

        MockMetadataStore(List<FileMetadata> list) {
            this.list = list;
        }

        @Override
        public List<FileMetadata> getAllMetadata() {
            return new ArrayList<>(list);
        }
    }

    @Test
    void testGetSummary_CalculatesAccurateMetrics() {
        List<FileMetadata> testData = List.of(
                new FileMetadata("docs/file1.txt", "hash-aaa", "backup_storage/hash-aaa.zip", 1000L, 400L, "2026-08-31T10:00:00", 12345L, false),
                new FileMetadata("docs/file2.txt", "hash-aaa", "backup_storage/hash-aaa.zip", 1000L, 400L, "2026-08-31T10:00:00", 12345L, false), // Duplicate of file1
                new FileMetadata("images/photo.png", "hash-bbb", "backup_storage/hash-bbb.zip", 3000L, 1600L, "2026-08-31T11:00:00", 67890L, false)
        );

        MockMetadataStore mockStore = new MockMetadataStore(testData);
        CatalogService catalogService = new CatalogServiceImpl(mockStore, jobRegistry);

        CatalogSummaryResponse summary = catalogService.getSummary();

        assertNotNull(summary);
        assertEquals(3, summary.getTotalFiles());
        assertEquals(2, summary.getUniqueFiles());
        assertEquals(5000L, summary.getTotalOriginalBytes()); // 1000 + 1000 + 3000
        assertEquals(2000L, summary.getTotalStoredBytes());   // 400 (unique hash-aaa) + 1600 (unique hash-bbb)
        assertEquals(3000L, summary.getDeduplicatedBytes()); // 5000 - 2000
        assertEquals(60.0, summary.getSpaceSavedPercentage(), 0.01);
        assertEquals("2026-08-31T11:00:00", summary.getLastBackupTime());
    }

    @Test
    void testGetSummary_EmptyCatalog_ReturnsZeroes() {
        MockMetadataStore mockStore = new MockMetadataStore(List.of());
        CatalogService catalogService = new CatalogServiceImpl(mockStore, jobRegistry);

        CatalogSummaryResponse summary = catalogService.getSummary();

        assertNotNull(summary);
        assertEquals(0, summary.getTotalFiles());
        assertEquals(0, summary.getUniqueFiles());
        assertEquals(0L, summary.getTotalOriginalBytes());
        assertEquals(0L, summary.getTotalStoredBytes());
        assertEquals(0L, summary.getDeduplicatedBytes());
        assertEquals(0.0, summary.getSpaceSavedPercentage());
        assertNull(summary.getLastBackupTime());
    }

    @Test
    void testGetFiles_PaginationAndFiltering() {
        List<FileMetadata> testData = List.of(
                new FileMetadata("docs/alpha.txt", "hash-1", "backup/hash-1.zip", 100L, 50L, "2026-08-31T10:00:00", 1L, false),
                new FileMetadata("docs/beta.txt", "hash-2", "backup/hash-2.zip", 200L, 100L, "2026-08-31T10:00:00", 2L, false),
                new FileMetadata("images/gamma.png", "hash-1", "backup/hash-1.zip", 100L, 50L, "2026-08-31T10:00:00", 3L, false), // duplicate hash-1
                new FileMetadata("logs/delta.log", "hash-3", "backup/hash-3.zip", 400L, 200L, "2026-08-31T10:00:00", 4L, false)
        );

        MockMetadataStore mockStore = new MockMetadataStore(testData);
        CatalogService catalogService = new CatalogServiceImpl(mockStore, jobRegistry);

        // Test path filtering
        CatalogPageResponse<CatalogFileResponse> docsOnly = catalogService.getFiles("docs", null, 0, 10);
        assertEquals(2, docsOnly.getTotalElements());
        assertEquals(2, docsOnly.getContent().size());

        // Test hash filtering & deduplication flag
        CatalogPageResponse<CatalogFileResponse> hash1Only = catalogService.getFiles(null, "hash-1", 0, 10);
        assertEquals(2, hash1Only.getTotalElements());
        assertTrue(hash1Only.getContent().get(0).isDeduplicated());
        assertTrue(hash1Only.getContent().get(1).isDeduplicated());

        // Test pagination slicing
        CatalogPageResponse<CatalogFileResponse> page0 = catalogService.getFiles(null, null, 0, 2);
        assertEquals(4, page0.getTotalElements());
        assertEquals(2, page0.getContent().size());
        assertEquals(2, page0.getTotalPages());
        assertTrue(page0.isHasMore());

        CatalogPageResponse<CatalogFileResponse> page1 = catalogService.getFiles(null, null, 1, 2);
        assertEquals(4, page1.getTotalElements());
        assertEquals(2, page1.getContent().size());
        assertFalse(page1.isHasMore());
    }
}

