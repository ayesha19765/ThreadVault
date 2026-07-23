package scanner;

import java.nio.file.Path;

public class FileTask {

    private final Path filePath;
    private final long size;

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

    @Override
    public String toString() {
        return String.format("%s (%d bytes)", filePath, size);
    }
}
