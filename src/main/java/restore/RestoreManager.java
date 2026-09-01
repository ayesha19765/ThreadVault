package restore;

import metadata.FileMetadata;
import metadata.MetadataStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Restores backed-up files using metadata information.
 *
 * Reads backup locations from MetadataStore,
 * extracts ZIP files, and recreates the original
 * directory structure inside the restore folder.
 */
public class RestoreManager {

    private static final Path DEFAULT_RESTORE_DIRECTORY =
            Path.of("restore");

    private static final int BUFFER_SIZE = 8192;

    private final Path restoreDirectory;
    private final MetadataStore metadataStore;

    public RestoreManager() {
        this(DEFAULT_RESTORE_DIRECTORY, new MetadataStore());
    }

    public RestoreManager(MetadataStore metadataStore) {
        this(DEFAULT_RESTORE_DIRECTORY, metadataStore);
    }

    public RestoreManager(Path restoreDirectory) {
        this(restoreDirectory, new MetadataStore());
    }

    public RestoreManager(Path restoreDirectory, MetadataStore metadataStore) {
        this.restoreDirectory = restoreDirectory != null ? restoreDirectory : DEFAULT_RESTORE_DIRECTORY;
        this.metadataStore = metadataStore != null ? metadataStore : new MetadataStore();

        try {
            Files.createDirectories(this.restoreDirectory);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to create restore directory: "
                            + this.restoreDirectory,
                    e
            );
        }
    }

    public int restoreAll() {
        List<FileMetadata> metadataList =
                metadataStore.getAllMetadata();

        int restoredCount = 0;
        for (FileMetadata metadata : metadataList) {
            restore(metadata);
            restoredCount++;
        }

        return restoredCount;
    }

    public Path getRestoreDirectory() {
        return restoreDirectory;
    }

    private void restore(
            FileMetadata metadata
    ) {
        Path zipFile =
                Path.of(metadata.getBackupPath());

        if (!Files.exists(zipFile)) {
            System.err.printf("[Restore] Warning: Backup archive not found for %s (%s). Skipping.%n",
                    metadata.getOriginalPath(), zipFile);
            return;
        }

        Path original = Path.of(metadata.getOriginalPath());
        Path relativePath = original.isAbsolute()
                ? (original.getRoot() != null ? original.getRoot().relativize(original) : original)
                : original;

        Path baseDir = restoreDirectory.toAbsolutePath().normalize();
        Path outputFile = baseDir.resolve(relativePath).normalize().toAbsolutePath();

        /*
         * Prevent path traversal attacks by ensuring
         * restored files remain strictly inside the restore folder.
         */
        if (!outputFile.startsWith(baseDir)) {
            throw new SecurityException(
                    "Path traversal attempt detected: "
                            + metadata.getOriginalPath()
            );
        }

        try {
            if (outputFile.getParent() != null) {
                Files.createDirectories(
                        outputFile.getParent()
                );
            }

            try (
                    InputStream input =
                            Files.newInputStream(zipFile);

                    ZipInputStream zis =
                            new ZipInputStream(input)
            ) {
                ZipEntry entry =
                        zis.getNextEntry();

                if (entry == null) {
                    return;
                }

                try (
                        OutputStream output =
                                Files.newOutputStream(outputFile)
                ) {
                    byte[] buffer =
                            new byte[BUFFER_SIZE];

                    int bytesRead;

                    while ((bytesRead = zis.read(buffer)) != -1) {
                        output.write(
                                buffer,
                                0,
                                bytesRead
                        );
                    }
                }
            }

            System.out.printf(
                    "[Restore] Restored : %s%n",
                    outputFile
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to restore file: "
                            + metadata.getOriginalPath(),
                    e
            );
        }
    }
}
