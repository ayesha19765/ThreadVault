package backup;

import dedup.DeduplicationEngine;
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
    private static final int QUEUE_CAPACITY = 100;

    public void startBackup(String folderPath) {

        System.out.println("======================================");
        System.out.println("      Mini Backup Engine");
        System.out.println("======================================");
        System.out.println("Scanning Folder : " + folderPath);
        System.out.println();

        // Shared queue between scanner and workers
        BlockingQueue<FileTask> queue =
                new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        // Shared deduplication engine
        DeduplicationEngine dedupEngine =
                new DeduplicationEngine();

        // Thread Pool
        ExecutorService executor =
                Executors.newFixedThreadPool(NUMBER_OF_WORKERS);

        // Start workers
        for (int i = 0; i < NUMBER_OF_WORKERS; i++) {
            executor.submit(
                    new BackupWorker(queue, dedupEngine)
            );
        }

        // Scan files
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.scan(Path.of(folderPath), queue);

        // Tell every worker to stop
        try {

            for (int i = 0; i < NUMBER_OF_WORKERS; i++) {
                queue.put(FileTask.POISON_PILL);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.shutdown();

        try {

            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {

                executor.shutdownNow();

            }

        } catch (InterruptedException e) {

            executor.shutdownNow();
            Thread.currentThread().interrupt();

        }

        System.out.println();
        System.out.println("======================================");
        System.out.println(" Backup Completed Successfully");
        System.out.println("======================================");
    }
}