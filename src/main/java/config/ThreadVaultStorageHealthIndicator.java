package config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Custom Spring Boot Actuator HealthIndicator verifying accessibility
 * and write-permissions of backup storage and metadata catalogs.
 */
@Component
public class ThreadVaultStorageHealthIndicator implements HealthIndicator {

    private final Path storagePath;
    private final Path metadataPath;

    public ThreadVaultStorageHealthIndicator(
            @Value("${threadvault.storage-path:backup_storage}") String storageDir,
            @Value("${threadvault.metadata-path:metadata}") String metadataDir
    ) {
        this.storagePath = Path.of(storageDir);
        this.metadataPath = Path.of(metadataDir);
    }

    @Override
    public Health health() {
        try {
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }
            if (!Files.exists(metadataPath)) {
                Files.createDirectories(metadataPath);
            }

            File storageFile = storagePath.toFile();
            File metadataFile = metadataPath.toFile();

            boolean storageWritable = storageFile.canWrite();
            boolean metadataWritable = metadataFile.canWrite();

            if (!storageWritable || !metadataWritable) {
                return Health.down()
                        .withDetail("storagePath", storagePath.toString())
                        .withDetail("storageWritable", storageWritable)
                        .withDetail("metadataPath", metadataPath.toString())
                        .withDetail("metadataWritable", metadataWritable)
                        .withDetail("error", "Storage or metadata directories are not writable")
                        .build();
            }

            long freeSpaceBytes = storageFile.getUsableSpace();

            return Health.up()
                    .withDetail("storagePath", storagePath.toString())
                    .withDetail("metadataPath", metadataPath.toString())
                    .withDetail("freeDiskSpaceBytes", freeSpaceBytes)
                    .withDetail("status", "Storage subsystem operational and writable")
                    .build();

        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("storagePath", storagePath.toString())
                    .withDetail("metadataPath", metadataPath.toString())
                    .build();
        }
    }
}

