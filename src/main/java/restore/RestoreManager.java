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

    private static final Path RESTORE_DIRECTORY =
            Path.of("restore");

    private static final int BUFFER_SIZE = 8192;

    private final MetadataStore metadataStore;

    public RestoreManager() {

        this.metadataStore = new MetadataStore();

        try {

            Files.createDirectories(RESTORE_DIRECTORY);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to create restore directory: "
                            + RESTORE_DIRECTORY,
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

    private void restore(
            FileMetadata metadata
    ) {

        Path zipFile =
                Path.of(metadata.getBackupPath());

        Path original = Path.of(metadata.getOriginalPath());
        Path relativePath = original.isAbsolute()
                ? (original.getRoot() != null ? original.getRoot().relativize(original) : original)
                : original;

        Path outputFile =
                RESTORE_DIRECTORY
                        .resolve(relativePath)
                        .normalize();

        /*
         * Prevent path traversal attacks by ensuring
         * restored files remain inside the restore folder.
         */
        if (!outputFile.startsWith(RESTORE_DIRECTORY.normalize())) {

            throw new RuntimeException(
                    "Invalid restore path: "
                            + outputFile
            );

        }

        try {

            Files.createDirectories(
                    outputFile.getParent()
            );

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
