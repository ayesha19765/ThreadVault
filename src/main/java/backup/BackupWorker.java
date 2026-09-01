package backup;

import compression.CompressionManager;
import dedup.DeduplicationEngine;
import dedup.HashCalculator;
import event.BackupEvent;
import event.BackupEventPublisher;
import event.BackupEventType;
import incremental.IncrementalBackupEngine;
import metadata.FileMetadata;
import scanner.FileTask;
import stats.BackupStatistics;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;

public class BackupWorker implements Runnable {

    private static final int MAX_RETRIES = 3;

    private final BlockingQueue<FileTask> fileQueue;
    private final BlockingQueue<FileMetadata> metadataQueue;

    private final DeduplicationEngine deduplicationEngine;
    private final CompressionManager compressionManager;
    private final IncrementalBackupEngine incrementalBackupEngine;
    private final HashCalculator hashCalculator;
    private final BackupStatistics statistics;

    private final String backupId;
    private final BackupEventPublisher eventPublisher;

    public BackupWorker(
            BlockingQueue<FileTask> fileQueue,
            BlockingQueue<FileMetadata> metadataQueue,
            DeduplicationEngine deduplicationEngine,
            CompressionManager compressionManager,
            IncrementalBackupEngine incrementalBackupEngine,
            BackupStatistics statistics
    ) {
        this(fileQueue, metadataQueue, deduplicationEngine, compressionManager, incrementalBackupEngine, statistics, null, null);
    }

    public BackupWorker(
            BlockingQueue<FileTask> fileQueue,
            BlockingQueue<FileMetadata> metadataQueue,
            DeduplicationEngine deduplicationEngine,
            CompressionManager compressionManager,
            IncrementalBackupEngine incrementalBackupEngine,
            BackupStatistics statistics,
            String backupId,
            BackupEventPublisher eventPublisher
    ) {
        this.fileQueue = fileQueue;
        this.metadataQueue = metadataQueue;
        this.deduplicationEngine = deduplicationEngine;
        this.compressionManager = compressionManager;
        this.incrementalBackupEngine = incrementalBackupEngine;
        this.statistics = statistics;
        this.backupId = backupId;
        this.eventPublisher = eventPublisher;

        this.hashCalculator = new HashCalculator();
    }

    @Override
    public void run() {

        final String workerName =
                Thread.currentThread().getName();

        try {

            while (true) {

                FileTask task = fileQueue.take();

                if (task == FileTask.POISON_PILL) {

                    System.out.printf(
                            "[%s] Shutting down.%n",
                            workerName
                    );

                    break;
                }

                processFile(task, workerName);

            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

        }

    }

    private void processFile(
            FileTask task,
            String workerName
    ) throws InterruptedException {

        statistics.fileScanned();

        final Path file = task.getFilePath();

        int attempts = 0;

        while (attempts < MAX_RETRIES) {

            try {

                /*
                 * Incremental Backup Check
                 */
                if (!incrementalBackupEngine.shouldBackup(file)) {

                    System.out.printf(
                            "[%s] Unchanged : %s%n",
                            workerName,
                            file.getFileName()
                    );

                    statistics.incrementalSkipped();

                    publishEvent(BackupEventType.FILE_SKIPPED, file.toString(), task.getSize());

                    return;
                }

                /*
                 * Calculate SHA-256
                 */
                final String hash =
                        hashCalculator.calculateSHA256(file);

                /*
                 * Deduplication Check
                 */
                if (deduplicationEngine.isDuplicate(hash, file)) {

                    System.out.printf(
                            "[%s] Duplicate Skipped : %s%n",
                            workerName,
                            file.getFileName()
                    );

                    statistics.duplicateSkipped();

                    // Re-use existing compressed archive for metadata indexing & restore
                    final Path backupLocation =
                            compressionManager.compress(
                                    file,
                                    hash
                            );

                    final long compressedSize =
                            Files.size(backupLocation);

                    final long lastModified =
                            Files.getLastModifiedTime(file)
                                    .toMillis();

                    FileMetadata metadata =
                            new FileMetadata(
                                    file.toString(),
                                    hash,
                                    backupLocation.toString(),
                                    task.getSize(),
                                    compressedSize,
                                    LocalDateTime.now().toString(),
                                    lastModified,
                                    false
                            );

                    metadataQueue.put(metadata);

                    publishEvent(BackupEventType.FILE_DEDUPLICATED, file.toString(), task.getSize());

                    return;
                }

                /*
                 * Compress File
                 */
                final Path backupLocation =
                        compressionManager.compress(
                                file,
                                hash
                        );

                final long compressedSize =
                        Files.size(backupLocation);

                final long lastModified =
                        Files.getLastModifiedTime(file)
                                .toMillis();

                /*
                 * Create Metadata
                 */
                FileMetadata metadata =
                        new FileMetadata(

                                file.toString(),

                                hash,

                                backupLocation.toString(),

                                task.getSize(),

                                compressedSize,

                                LocalDateTime.now().toString(),

                                lastModified,

                                false

                        );

                /*
                 * Send Metadata
                 */
                metadataQueue.put(metadata);

                statistics.fileBackedUp(
                        task.getSize(),
                        compressedSize
                );

                System.out.printf(
                        "[%s] Backed Up : %s -> %s%n",
                        workerName,
                        file.getFileName(),
                        backupLocation.getFileName()
                );

                publishEvent(BackupEventType.FILE_PROCESSED, file.toString(), task.getSize());

                /*
                 * SUCCESS
                 */
                return;

            }

            catch (Exception e) {

                Thread.sleep(
                        (long) Math.pow(2, attempts) * 500
                );

                attempts++;

                System.err.printf(
                        "[%s] Retry %d/%d : %s%n",
                        workerName,
                        attempts,
                        MAX_RETRIES,
                        file.getFileName()
                );

                if (attempts == MAX_RETRIES) {

                    statistics.fileFailed();

                    System.err.printf(
                            "[%s] FAILED : %s%n",
                            workerName,
                            file
                    );

                    publishEvent(BackupEventType.FILE_FAILED, file.toString(), task.getSize());

                }

            }

        }

    }

    private void publishEvent(BackupEventType type, String filePath, long fileSize) {
        if (eventPublisher != null && backupId != null) {
            double spaceSaved = 0.0;
            if (statistics.getOriginalBytes() > 0) {
                double compPct = (statistics.getCompressedBytes() * 100.0) / statistics.getOriginalBytes();
                spaceSaved = Math.max(0.0, Math.round((100.0 - compPct) * 100.0) / 100.0);
            }

            BackupEvent event = BackupEvent.builder(backupId, type)
                    .file(filePath)
                    .fileSize(fileSize)
                    .stats(
                            statistics.getFilesScanned(),
                            statistics.getFilesBackedUp(),
                            statistics.getIncrementalSkipped(),
                            statistics.getDuplicatesSkipped(),
                            statistics.getFailedFiles(),
                            statistics.getCompressedBytes(),
                            spaceSaved
                    )
                    .build();

            eventPublisher.publish(event);
        }
    }

}