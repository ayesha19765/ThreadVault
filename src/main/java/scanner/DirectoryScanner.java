package scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

public class DirectoryScanner {

    public void scan(Path root,
                     BlockingQueue<FileTask> queue) {

        try (var paths = Files.walk(root)) {

            paths.filter(Files::isRegularFile)
                    .forEach(path -> {

                        try {

                            queue.put(
                                    new FileTask(
                                            path,
                                            Files.size(path)));

                        } catch (Exception e) {

                            e.printStackTrace();

                        }

                    });

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }

}