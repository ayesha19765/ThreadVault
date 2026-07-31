package metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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
 * original file path.
 */
public class MetadataStore {

    private static final Path METADATA_FOLDER =
            Path.of("metadata");

    private static final Path METADATA_FILE_PATH =
            METADATA_FOLDER.resolve("metadata.json");

    private final ObjectMapper mapper;

    private final ConcurrentHashMap<String, FileMetadata>
            metadataIndex;

    public MetadataStore() {

        try {

            Files.createDirectories(METADATA_FOLDER);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to create metadata directory.",
                    e
            );

        }

        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);

        this.metadataIndex = loadMetadata();

    }

    private ConcurrentHashMap<String, FileMetadata> loadMetadata() {

        final ConcurrentHashMap<String, FileMetadata> index =
                new ConcurrentHashMap<>();

        if (!Files.exists(METADATA_FILE_PATH)) {

            return index;

        }

        try {

            List<FileMetadata> metadataList =
                    mapper.readValue(
                            METADATA_FILE_PATH.toFile(),
                            mapper.getTypeFactory()
                                    .constructCollectionType(
                                            List.class,
                                            FileMetadata.class
                                    )
                    );

            for (FileMetadata metadata : metadataList) {

                index.put(
                        metadata.getOriginalPath(),
                        metadata
                );

            }

        } catch (IOException e) {

            System.err.println(
                    "Warning: Unable to read metadata. Starting with an empty catalog."
            );

            return index;

        }

        return index;

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
                    METADATA_FILE_PATH.toFile(),
                    metadataSnapshot
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to write metadata.json",
                    e
            );

        }

    }

    public List<FileMetadata> getAllMetadata() {

        return new ArrayList<>(
                metadataIndex.values()
        );

    }

    public FileMetadata getMetadata(
            String originalPath
    ) {

        return metadataIndex.get(
                originalPath
        );

    }

    public boolean contains(
            String originalPath
    ) {

        return metadataIndex.containsKey(
                originalPath
        );

    }

}