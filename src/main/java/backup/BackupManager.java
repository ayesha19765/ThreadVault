package backup;

import scanner.DirectoryScanner;
import scanner.FileTask;

import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BackupManager {

    private static final int NUMBER_OF_WORKERS = 4;

    public void startBackup(String folderPath) {

        System.out.println("==================================");
        System.out.println("Mini Backup Engine");
        System.out.println("==================================");

        BlockingQueue<FileTask> queue =
                new ArrayBlockingQueue<>(100);

        ExecutorService executor =
                Executors.newFixedThreadPool(NUMBER_OF_WORKERS);

        for (int i = 0; i < NUMBER_OF_WORKERS; i++) {
            executor.submit(new BackupWorker(queue));
        }

        DirectoryScanner scanner = new DirectoryScanner();

        scanner.scan(Path.of(folderPath), queue);

        for (int i = 0; i < NUMBER_OF_WORKERS; i++) {

            try {
                queue.put(FileTask.POISON_PILL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        }

        executor.shutdown();

        try {

            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }

        } catch (InterruptedException e) {

            executor.shutdownNow();
            Thread.currentThread().interrupt();

        }

        System.out.println("\nBackup Finished Successfully.");
    }

}
