package config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ThreadVaultStorageHealthIndicatorTest {

    @TempDir
    Path tempDir;

    @Test
    void testHealth_StorageAndMetadataWritable_ReturnsUp() {
        Path storage = tempDir.resolve("backup_storage");
        Path metadata = tempDir.resolve("metadata");

        ThreadVaultStorageHealthIndicator indicator =
                new ThreadVaultStorageHealthIndicator(storage.toString(), metadata.toString());

        Health health = indicator.health();
        assertNotNull(health);
        assertEquals(Status.UP, health.getStatus());
        assertTrue(health.getDetails().containsKey("storagePath"));
        assertTrue(health.getDetails().containsKey("metadataPath"));
        assertTrue(health.getDetails().containsKey("freeDiskSpaceBytes"));
    }
}

