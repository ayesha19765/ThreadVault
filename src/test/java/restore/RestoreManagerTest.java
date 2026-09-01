package restore;

import metadata.FileMetadata;
import metadata.MetadataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class RestoreManagerTest {

    @TempDir
    Path tempDir;

    private Path storageDir;
    private Path restoreDir;
    private Path metadataDir;
    private MetadataStore metadataStore;

    @BeforeEach
    void setUp() throws IOException {
        storageDir = tempDir.resolve("backup_storage");
        restoreDir = tempDir.resolve("restore");
        metadataDir = tempDir.resolve("metadata");
        Files.createDirectories(storageDir);
        Files.createDirectories(restoreDir);
        Files.createDirectories(metadataDir);

        // Custom metadata store isolated in temp directory
        metadataStore = new MetadataStore(metadataDir);
    }

    private Path createZipArchive(String entryName, byte[] content, String hash) throws IOException {
        Path zipFile = storageDir.resolve(hash + ".zip");
        try (OutputStream fos = Files.newOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            zos.write(content);
            zos.closeEntry();
        }
        return zipFile;
    }

    @Test
    void testPathTraversalAttackThrowsSecurityException() throws IOException {
        String hash = "dummyhash123";
        Path zipFile = createZipArchive("secret.txt", "hacked content".getBytes(), hash);

        // Create malicious metadata with ../../ traversal attempt
        FileMetadata maliciousMetadata = new FileMetadata(
                "../../outside.txt",
                hash,
                zipFile.toString(),
                14,
                Files.size(zipFile),
                "2026-08-31T00:00:00",
                System.currentTimeMillis(),
                false
        );

        metadataStore.saveMetadata(maliciousMetadata);
        RestoreManager manager = new RestoreManager(restoreDir, metadataStore);

        SecurityException thrown = assertThrows(
                SecurityException.class,
                manager::restoreAll,
                "Expected restoreAll to reject path traversal attempt"
        );
        assertTrue(thrown.getMessage().contains("Path traversal attempt detected"));
    }

    @Test
    void testByteForByteRestorationMatchesOriginalHash() throws Exception {
        byte[] originalContent = "ThreadVault Deterministic Verification Content 12345".getBytes();
        String hash = "a1b2c3d4e5f6";
        Path zipFile = createZipArchive("document.txt", originalContent, hash);

        FileMetadata metadata = new FileMetadata(
                "folder/subfolder/document.txt",
                hash,
                zipFile.toString(),
                originalContent.length,
                Files.size(zipFile),
                "2026-08-31T00:00:00",
                System.currentTimeMillis(),
                false
        );

        metadataStore.saveMetadata(metadata);
        RestoreManager manager = new RestoreManager(restoreDir, metadataStore);

        int restored = manager.restoreAll();
        assertEquals(1, restored);

        Path restoredFile = restoreDir.resolve("folder/subfolder/document.txt");
        assertTrue(Files.exists(restoredFile));
        assertArrayEquals(originalContent, Files.readAllBytes(restoredFile));
    }
}

