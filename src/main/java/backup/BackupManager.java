package backup;

import compression.CompressionManager;
import dedup.DeduplicationEngine;
import metadata.MetadataStore;
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

        BlockingQueue<FileTask> queue =
                new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        // Shared Components
        DeduplicationEngine deduplicationEngine =
                new DeduplicationEngine();

        CompressionManager compressionManager =
                new CompressionManager();

        MetadataStore metadataStore =
                new MetadataStore();

        ExecutorService executor =
                Executors.newFixedThreadPool(NUMBER_OF_WORKERS);

        for (int i = 0; i < NUMBER_OF_WORKERS; i++) {

            executor.submit(
                    new BackupWorker(
                            queue,
                            deduplicationEngine,
                            compressionManager,
                            metadataStore
                    )
            );

        }

        DirectoryScanner scanner = new DirectoryScanner();

        scanner.scan(Path.of(folderPath), queue);

        // Send Poison Pills
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