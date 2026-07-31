package incremental;

import metadata.FileMetadata;
import metadata.MetadataStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Determines whether a file requires backup
 * by comparing current filesystem metadata
 * with previously stored backup metadata.
 *
 * Avoids expensive hashing and compression
 * for unchanged files.
 */
public class IncrementalBackupEngine {

    private final MetadataStore metadataStore;

    public IncrementalBackupEngine(
            MetadataStore metadataStore
    ) {

        this.metadataStore = metadataStore;

    }

    public boolean shouldBackup(
            Path file
    ) throws IOException {

        FileMetadata previous =
                metadataStore.getMetadata(
                        file.toString()
                );

        if (previous == null) {

            return true;

        }

        long currentSize =
                Files.size(file);

        long currentModified =
                Files.getLastModifiedTime(file)
                        .toMillis();

        return currentSize != previous.getOriginalSize()
                ||
                currentModified != previous.getLastModifiedTime();

    }

}