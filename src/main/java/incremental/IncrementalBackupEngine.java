package incremental;

import metadata.FileMetadata;
import metadata.MetadataStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class IncrementalBackupEngine {

    private final MetadataStore metadataStore;

    public IncrementalBackupEngine(
            MetadataStore metadataStore) {

        this.metadataStore = metadataStore;

    }

    public boolean shouldBackup(Path file)
            throws Exception {

        FileMetadata previous =
                metadataStore.getMetadata(
                        file.toString());

        if (previous == null)
            return true;

        long currentSize =
                Files.size(file);

        long currentModified =
                Files.getLastModifiedTime(file)
                        .toMillis();

        return currentSize != previous.getOriginalSize()

                ||

                currentModified !=
                        previous.getLastModifiedTime();

    }

}