package backup;

import compression.CompressionManager;
import dedup.DeduplicationEngine;
import metadata.FileMetadata;
import metadata.MetadataStore;
import metadata.MetadataWriter;
import scanner.DirectoryScanner;
import scanner.FileTask;

import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import incremental.IncrementalBackupEngine;
import stats.BackupStatistics;

public class BackupManager {

    private static final int NUMBER_OF_WORKERS = 4;
    private static final int FILE_QUEUE_CAPACITY = 100;
    private static final int METADATA_QUEUE_CAPACITY = 100;

    public void startBackup(String folderPath) {

        System.out.println("======================================");
        System.out.println("       Mini Backup Engine");
        System.out.println("======================================");
        System.out.println("Scanning Folder : " + folderPath);
        System.out.println();

        /*
         * Queue containing files waiting to be backed up
         */
        BlockingQueue<FileTask> fileQueue =
                new ArrayBlockingQueue<>(FILE_QUEUE_CAPACITY);

        /*
         * Queue containing metadata waiting to be written
         */
        BlockingQueue<FileMetadata> metadataQueue =
                new ArrayBlockingQueue<>(METADATA_QUEUE_CAPACITY);

        /*
         * Shared Components
         */
        DeduplicationEngine deduplicationEngine =
                new DeduplicationEngine();

        CompressionManager compressionManager =
                new CompressionManager();

        MetadataStore metadataStore =
                new MetadataStore();

        BackupStatistics statistics =
                new BackupStatistics();

        IncrementalBackupEngine incrementalBackupEngine =
                new IncrementalBackupEngine(
                        metadataStore
                );

        /*
         * Dedicated Metadata Writer Thread
         */
        Thread metadataWriterThread =
                new Thread(
                        new MetadataWriter(
                                metadataQueue,
                                metadataStore
                        ),
                        "Metadata-Writer"
                );

        metadataWriterThread.start();

        /*
         * Thread Pool for Backup Workers
         */
        ExecutorService executor =
                Executors.newFixedThreadPool(NUMBER_OF_WORKERS);

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
         * Scan the directory and enqueue files
         */
        DirectoryScanner scanner = new DirectoryScanner();

        scanner.scan(Path.of(folderPath), fileQueue);

        /*
         * Signal workers to stop
         */
        try {

            for (int i = 0; i < NUMBER_OF_WORKERS; i++) {

                fileQueue.put(FileTask.POISON_PILL);

            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

        }

        /*
         * Wait for all workers
         */
        executor.shutdown();

        try {

            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {

                executor.shutdownNow();

            }

        } catch (InterruptedException e) {

            executor.shutdownNow();
            Thread.currentThread().interrupt();

        }

        /*
         * Tell Metadata Writer that no more metadata is coming
         */
        try {

            metadataQueue.put(MetadataWriter.POISON);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

        }

        /*
         * Wait for Metadata Writer to finish
         */
        try {

            metadataWriterThread.join();

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

        }

        System.out.println();
        System.out.println("========== Backup Statistics ==========");

        System.out.println(
                "Files Scanned        : "
                        + statistics.getFilesScanned());

        System.out.println(
                "Files Backed Up      : "
                        + statistics.getFilesBackedUp());

        System.out.println(
                "Duplicates Skipped   : "
                        + statistics.getDuplicatesSkipped());

        System.out.println(
                "Incremental Skipped  : "
                        + statistics.getIncrementalSkipped());

        System.out.println(
                "Original Size (Bytes): "
                        + statistics.getOriginalBytes());

        System.out.println(
                "Compressed Size(Bytes): "
                        + statistics.getCompressedBytes());

        double ratio = 100.0;

        if (statistics.getOriginalBytes() != 0) {

            ratio = (statistics.getCompressedBytes() * 100.0)
                    / statistics.getOriginalBytes();

        }

        System.out.printf(
                "Compression Ratio    : %.2f%%%n",
                ratio);

        System.out.println("=======================================");

        System.out.println();
        System.out.println("======================================");
        System.out.println(" Backup Completed Successfully");
        System.out.println("======================================");
    }
}