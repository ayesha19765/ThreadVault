package metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores backup metadata in memory and persists it
 * to metadata/metadata.json.
 *
 * Uses a ConcurrentHashMap for O(1) lookups by
 * original file path and dynamically refreshes from disk
 * if updated.
 */
@Component
public class MetadataStore {

    private static Path getDefaultMetadataFolder() {
        return Path.of(System.getProperty("THREADVAULT_METADATA_PATH",
                System.getenv().getOrDefault("THREADVAULT_METADATA_PATH", "metadata")));
    }

    private final Path metadataFolder;
    private final Path metadataFilePath;
    private final ObjectMapper mapper;
    private final ConcurrentHashMap<String, FileMetadata> metadataIndex;
    private volatile long lastLoadedTime = 0L;

    public MetadataStore() {
        this(getDefaultMetadataFolder());
    }

    public MetadataStore(Path metadataFolder) {
        this.metadataFolder = metadataFolder != null ? metadataFolder : getDefaultMetadataFolder();
        this.metadataFilePath = this.metadataFolder.resolve("metadata.json");

        try {
            Files.createDirectories(this.metadataFolder);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to create metadata directory.",
                    e
            );
        }

        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);

        this.metadataIndex = new ConcurrentHashMap<>();
        reload();
    }

    public synchronized void reload() {
        if (!Files.exists(metadataFilePath)) {
            return;
        }

        try {
            List<FileMetadata> metadataList =
                    mapper.readValue(
                            metadataFilePath.toFile(),
                            mapper.getTypeFactory()
                                    .constructCollectionType(
                                            List.class,
                                            FileMetadata.class
                                    )
                    );

            metadataIndex.clear();
            for (FileMetadata metadata : metadataList) {
                if (metadata.getOriginalPath() != null) {
                    metadataIndex.put(
                            metadata.getOriginalPath(),
                            metadata
                    );
                }
            }
            lastLoadedTime = Files.getLastModifiedTime(metadataFilePath).toMillis();
        } catch (IOException e) {
            System.err.println(
                    "Warning: Unable to read metadata. Starting with an empty catalog."
            );
        }
    }

    private void checkRefresh() {
        if (Files.exists(metadataFilePath)) {
            try {
                long diskModified = Files.getLastModifiedTime(metadataFilePath).toMillis();
                if (diskModified > lastLoadedTime) {
                    reload();
                }
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized void saveMetadata(FileMetadata metadata) {
        metadataIndex.put(
                metadata.getOriginalPath(),
                metadata
        );

        List<FileMetadata> metadataSnapshot =
                new ArrayList<>(metadataIndex.values());

        try {
            mapper.writeValue(
                    metadataFilePath.toFile(),
                    metadataSnapshot
            );
            if (Files.exists(metadataFilePath)) {
                lastLoadedTime = Files.getLastModifiedTime(metadataFilePath).toMillis();
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to write metadata.json",
                    e
            );
        }
    }

    public List<FileMetadata> getAllMetadata() {
        checkRefresh();
        return new ArrayList<>(
                metadataIndex.values()
        );
    }

    public FileMetadata getMetadata(
            String originalPath
    ) {
        checkRefresh();
        return metadataIndex.get(
                originalPath
        );
    }

    public boolean contains(
            String originalPath
    ) {
        checkRefresh();
        return metadataIndex.containsKey(
                originalPath
        );
    }
}
