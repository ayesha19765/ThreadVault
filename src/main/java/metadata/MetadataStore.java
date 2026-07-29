package metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MetadataStore {

    private static final Path METADATA_DIRECTORY =
            Path.of("metadata");

    private static final Path METADATA_FILE =
            METADATA_DIRECTORY.resolve("metadata.json");

    private final ObjectMapper mapper;

    private final List<FileMetadata> metadataList;

    public MetadataStore() {

        try {

            Files.createDirectories(METADATA_DIRECTORY);

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

        mapper = new ObjectMapper();

        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        metadataList = loadMetadata();

    }

    private List<FileMetadata> loadMetadata() {

        if (!Files.exists(METADATA_FILE))
            return new ArrayList<>();

        try {

            return mapper.readValue(
                    METADATA_FILE.toFile(),
                    mapper.getTypeFactory()
                            .constructCollectionType(
                                    List.class,
                                    FileMetadata.class));

        } catch (IOException e) {

            return new ArrayList<>();

        }

    }

    public synchronized void saveMetadata(FileMetadata metadata) {

        metadataList.add(metadata);

        try {

            mapper.writeValue(
                    METADATA_FILE.toFile(),
                    metadataList);

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }

}