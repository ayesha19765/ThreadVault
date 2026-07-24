package scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DirectoryScanner {

    public List<FileTask> scan(Path rootDirectory) {

        List<FileTask> files = new ArrayList<>();

        try (var paths = Files.walk(rootDirectory)) {

            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            files.add(new FileTask(path, Files.size(path)));
                        } catch (IOException e) {
                            System.err.println("Unable to read file: " + path);
                        }
                    });

        } catch (IOException e) {
            throw new RuntimeException("Failed to scan directory: " + rootDirectory, e);
        }

        return files;
    }
}