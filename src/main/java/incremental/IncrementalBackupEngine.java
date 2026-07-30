package incremental;

import metadata.FileMetadata;
import metadata.MetadataStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class IncrementalBackupEngine {

    private final Map<String, FileMetadata> previousBackups =
            new HashMap<>();

    public IncrementalBackupEngine(
            MetadataStore metadataStore) {

        for (FileMetadata metadata :
                metadataStore.getAllMetadata()) {

            previousBackups.put(
                    metadata.getOriginalPath(),
                    metadata);

        }

    }

    public boolean shouldBackup(Path file)
            throws Exception {

        FileMetadata previous =
                previousBackups.get(file.toString());

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