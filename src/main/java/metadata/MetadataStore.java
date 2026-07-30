package metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MetadataStore {

    private static final Path METADATA_DIRECTORY =
            Path.of("metadata");

    private static final Path METADATA_FILE =
            METADATA_DIRECTORY.resolve("metadata.json");

    private final ObjectMapper mapper;

    private final ConcurrentHashMap<String, FileMetadata>
            metadataIndex;

    public MetadataStore() {

        try {

            Files.createDirectories(METADATA_DIRECTORY);

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

        mapper = new ObjectMapper();

        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        metadataIndex = loadMetadata();

    }

    private ConcurrentHashMap<String, FileMetadata> loadMetadata() {

        ConcurrentHashMap<String, FileMetadata> index =
                new ConcurrentHashMap<>();

        if (!Files.exists(METADATA_FILE))
            return index;

        try {

            List<FileMetadata> metadataList =
                    mapper.readValue(
                            METADATA_FILE.toFile(),
                            mapper.getTypeFactory()
                                    .constructCollectionType(
                                            List.class,
                                            FileMetadata.class));

            for (FileMetadata metadata : metadataList) {

                index.put(
                        metadata.getOriginalPath(),
                        metadata
                );

            }

        } catch (IOException e) {

            return index;

        }

        return index;
    }

    public synchronized void saveMetadata(FileMetadata metadata) {

        metadataIndex.put(
                metadata.getOriginalPath(),
                metadata
        );

        try {

            mapper.writeValue(

                    METADATA_FILE.toFile(),

                    metadataIndex.values()

            );

        }

        catch (IOException e) {

            throw new RuntimeException(e);

        }

    }

    public List<FileMetadata> getAllMetadata() {

        return new ArrayList<>(
                metadataIndex.values());

    }

    public FileMetadata getMetadata(
            String originalPath) {

        return metadataIndex.get(originalPath);

    }

    public boolean contains(
            String originalPath) {

        return metadataIndex.containsKey(
                originalPath);

    }
}