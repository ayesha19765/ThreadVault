package scanner;

import java.nio.file.Path;

/**
 * Represents a file waiting to be processed
 * by backup workers.
 *
 * Immutable object passed through the
 * producer-consumer queue.
 */
public class FileTask {

    private final Path filePath;
    private final long size;

    /*
     * Sentinel object used to notify workers
     * that scanning is complete.
     */
    public static final FileTask POISON_PILL =
            new FileTask(null, -1);

    public FileTask(
            Path filePath,
            long size
    ) {

        this.filePath = filePath;
        this.size = size;

    }

    public Path getFilePath() {

        return filePath;

    }

    public long getSize() {

        return size;

    }

    @Override
    public String toString() {

        return "FileTask{" +
                "filePath=" + filePath +
                ", size=" + size +
                '}';

    }

}