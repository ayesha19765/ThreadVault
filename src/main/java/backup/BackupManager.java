package backup;

import compression.CompressionManager;
import dedup.DeduplicationEngine;
import incremental.IncrementalBackupEngine;
import metadata.FileMetadata;
import metadata.MetadataStore;
import metadata.MetadataWriter;
import scanner.DirectoryScanner;
import scanner.FileTask;
import stats.BackupStatistics;

import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BackupManager {

    private static final int NUMBER_OF_WORKERS = 4;
    private static final int FILE_QUEUE_CAPACITY = 100;
    private static final int METADATA_QUEUE_CAPACITY = 100;
    private static final long WORKER_TIMEOUT_MINUTES = 5;

    public void startBackup(String folderPath) {

        System.out.println("======================================");
        System.out.println("       Mini Backup Engine");
        System.out.println("======================================");
        System.out.println("Scanning Folder : " + folderPath);
        System.out.println();

        /*
         * Queue containing files waiting to be backed up.
         */
        final BlockingQueue<FileTask> fileQueue =
                new ArrayBlockingQueue<>(FILE_QUEUE_CAPACITY);

        /*
         * Queue containing metadata waiting to be written.
         */
        final BlockingQueue<FileMetadata> metadataQueue =
                new ArrayBlockingQueue<>(METADATA_QUEUE_CAPACITY);

        /*
         * Shared components used by all workers.
         */
        final DeduplicationEngine deduplicationEngine =
                new DeduplicationEngine();

        final CompressionManager compressionManager =
                new CompressionManager();

        final MetadataStore metadataStore =
                new MetadataStore();

        final BackupStatistics statistics =
                new BackupStatistics();

        final IncrementalBackupEngine incrementalBackupEngine =
                new IncrementalBackupEngine(metadataStore);

        /*
         * Dedicated metadata writer thread.
         */
        final Thread metadataWriterThread =
                new Thread(
                        new MetadataWriter(
                                metadataQueue,
                                metadataStore
                        ),
                        "Metadata-Writer"
                );

        metadataWriterThread.start();

        /*
         * Custom thread names for backup workers.
         */
        final AtomicInteger workerCounter = new AtomicInteger(1);

        ThreadFactory threadFactory = runnable -> {

            Thread thread = new Thread(runnable);

            thread.setName(
                    "BackupWorker-" + workerCounter.getAndIncrement()
            );

            return thread;
        };

        /*
         * Thread pool for backup workers.
         */
        final ExecutorService executor =
                Executors.newFixedThreadPool(
                        NUMBER_OF_WORKERS,
                        threadFactory
                );

        for (int i = 0; i < NUMBER_OF_WORKERS; i++) {

            executor.submit(
                    new BackupWorker(
                            fileQueue,
                            metadataQueue,
                            deduplicationEngine,
                            compressionManager,
                            incrementalBackupEngine,
                            statistics
                    )
            );

        }

        /*
         * Scan the directory and enqueue files.
         */
        final DirectoryScanner scanner = new DirectoryScanner();

        scanner.scan(Path.of(folderPath), fileQueue);

        /*
         * Signal workers to stop.
         */
        try {

            for (int i = 0; i < NUMBER_OF_WORKERS; i++) {

                fileQueue.put(FileTask.POISON_PILL);

            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

        }

        /*
         * Wait for all backup workers.
         */
        executor.shutdown();

        try {

            if (!executor.awaitTermination(
                    WORKER_TIMEOUT_MINUTES,
                    TimeUnit.MINUTES)) {

                executor.shutdownNow();

            }

        } catch (InterruptedException e) {

            executor.shutdownNow();
            Thread.currentThread().interrupt();

        }

        /*
         * Notify metadata writer that all metadata
         * has been produced.
         */
        try {

            metadataQueue.put(MetadataWriter.POISON);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

        }

        /*
         * Wait for metadata writer to finish.
         */
        try {

            metadataWriterThread.join();

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

        }

        /*
         * Print backup summary.
         */
        statistics.printSummary();

        System.out.println();
        System.out.println("======================================");
        System.out.println(" Backup Completed Successfully");
        System.out.println("======================================");

    }

}