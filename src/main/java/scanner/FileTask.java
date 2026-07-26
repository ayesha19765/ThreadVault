package scanner;

import java.nio.file.Path;

public class FileTask {

    private final Path filePath;
    private final long size;

    public static final FileTask POISON_PILL =
            new FileTask(null, -1);

    public FileTask(Path filePath, long size) {
        this.filePath = filePath;
        this.size = size;
    }

    public Path getFilePath() {
        return filePath;
    }

    public long getSize() {
        return size;
    }

}