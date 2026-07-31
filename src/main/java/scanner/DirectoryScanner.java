package scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

/**
 * Recursively scans a directory and publishes
 * discovered files into a blocking queue.
 *
 * Acts as the producer in the backup pipeline.
 */
public class DirectoryScanner {

    public void scan(
            Path root,
            BlockingQueue<FileTask> queue
    ) {

        try (var paths = Files.walk(root)) {

            paths.filter(Files::isRegularFile)
                    .forEach(path -> {

                        try {

                            queue.put(
                                    new FileTask(
                                            path,
                                            Files.size(path)
                                    )
                            );

                        } catch (IOException e) {

                            System.err.println(
                                    "Unable to read file size: "
                                            + path
                            );

                        } catch (InterruptedException e) {

                            System.err.println(
                                    "Directory scanner interrupted."
                            );

                            Thread.currentThread()
                                    .interrupt();

                        }

                    });

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to scan directory: "
                            + root,
                    e
            );

        }

    }

}