package backup;

import compression.CompressionManager;
import dedup.DeduplicationEngine;
import event.BackupEvent;
import event.BackupEventPublisher;
import event.BackupEventType;
import incremental.IncrementalBackupEngine;
import metadata.FileMetadata;
import metadata.MetadataStore;
import metadata.MetadataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(BackupManager.class);
    private static final int NUMBER_OF_WORKERS = 4;
    private static final int FILE_QUEUE_CAPACITY = 100;
    private static final int METADATA_QUEUE_CAPACITY = 100;
    private static final long WORKER_TIMEOUT_MINUTES = 5;

    public void startBackup(String folderPath) {
        startBackup(folderPath, NUMBER_OF_WORKERS, new BackupStatistics());
    }

    public BackupStatistics startBackup(String folderPath, int workerCount) {
        return startBackup(folderPath, workerCount, new BackupStatistics());
    }

    public BackupStatistics startBackup(String folderPath, int workerCount, BackupStatistics statistics) {
        return startBackup(folderPath, workerCount, statistics, null, null);
    }

    public BackupStatistics startBackup(
            String folderPath,
            int workerCount,
            BackupStatistics statistics,
            String backupId,
            BackupEventPublisher eventPublisher
    ) {

        final int workers = workerCount > 0 ? workerCount : NUMBER_OF_WORKERS;
        final BackupStatistics stats = statistics != null ? statistics : new BackupStatistics();

        logger.info("Starting backup for: {} with {} worker threads (jobId: {})", folderPath, workers, backupId);

        if (eventPublisher != null && backupId != null) {
            eventPublisher.publish(
                    BackupEvent.builder(backupId, BackupEventType.BACKUP_STARTED)
                            .message("Backup started for directory: " + folderPath)
                            .build()
            );
        }

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
        final MetadataStore metadataStore =
                new MetadataStore();

        final DeduplicationEngine deduplicationEngine =
                new DeduplicationEngine(metadataStore);

        final CompressionManager compressionManager =
                new CompressionManager();

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
                        workers,
                        threadFactory
                );

        for (int i = 0; i < workers; i++) {

            executor.submit(
                    new BackupWorker(
                            fileQueue,
                            metadataQueue,
                            deduplicationEngine,
                            compressionManager,
                            incrementalBackupEngine,
                            stats,
                            backupId,
                            eventPublisher
                    )
            );

        }

        /*
         * Scan the directory and enqueue files.
         */
        final DirectoryScanner scanner = new DirectoryScanner();

        scanner.scan(Path.of(folderPath), fileQueue, path -> {
            if (eventPublisher != null && backupId != null) {
                eventPublisher.publish(
                        BackupEvent.builder(backupId, BackupEventType.FILE_DISCOVERED)
                                .file(path.toString())
                                .stats(
                                        stats.getFilesScanned(),
                                        stats.getFilesBackedUp(),
                                        stats.getIncrementalSkipped(),
                                        stats.getDuplicatesSkipped(),
                                        stats.getFailedFiles(),
                                        stats.getCompressedBytes(),
                                        0.0
                                )
                                .build()
                );
            }
        });

        /*
         * Signal workers to stop.
         */
        try {

            for (int i = 0; i < workers; i++) {

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

        logger.info("Backup finished successfully for: {} (Scanned: {}, BackedUp: {}, Deduplicated: {}, IncrementalSkipped: {})",
                folderPath, stats.getFilesScanned(), stats.getFilesBackedUp(), stats.getDuplicatesSkipped(), stats.getIncrementalSkipped());

        return stats;
    }

}
