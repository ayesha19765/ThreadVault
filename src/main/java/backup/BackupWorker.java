package backup;

import compression.CompressionManager;
import dedup.DeduplicationEngine;
import dedup.HashCalculator;
import metadata.FileMetadata;
import scanner.FileTask;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;

import incremental.IncrementalBackupEngine;
import stats.BackupStatistics;

import java.nio.file.attribute.FileTime;

public class BackupWorker implements Runnable {

    private final BlockingQueue<FileTask> fileQueue;
    private final BlockingQueue<FileMetadata> metadataQueue;

    private final DeduplicationEngine deduplicationEngine;
    private final CompressionManager compressionManager;
    private final IncrementalBackupEngine incrementalBackupEngine;
    private final HashCalculator hashCalculator;
    private final BackupStatistics statistics;
    private static final int MAX_RETRIES = 3;

    public BackupWorker(
            BlockingQueue<FileTask> fileQueue,
            BlockingQueue<FileMetadata> metadataQueue,
            DeduplicationEngine deduplicationEngine,
            CompressionManager compressionManager, IncrementalBackupEngine incrementalBackupEngine,
            BackupStatistics statistics
    ) {

        this.fileQueue = fileQueue;
        this.metadataQueue = metadataQueue;
        this.deduplicationEngine = deduplicationEngine;
        this.compressionManager = compressionManager;
        this.incrementalBackupEngine = incrementalBackupEngine;
        this.hashCalculator = new HashCalculator();
        this.statistics = statistics;
    }

    @Override
    public void run() {

        String workerName = Thread.currentThread().getName();

        try {

            while (true) {

                FileTask task = fileQueue.take();

                if (task == FileTask.POISON_PILL) {

                    System.out.println(workerName + " shutting down.");

                    break;
                }

                processFile(task, workerName);
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

        }
    }

    private void processFile(FileTask task, String workerName) throws InterruptedException {
        int attempts = 0;
        statistics.fileScanned();
        while (attempts < MAX_RETRIES) {
            try {
                if (!incrementalBackupEngine.shouldBackup(
                        task.getFilePath())) {

                    System.out.printf(
                            "[%s] Unchanged : %s%n",
                            workerName,
                            task.getFilePath().getFileName());
                    statistics.incrementalSkipped();
                    return;
                }
                // Calculate SHA-256
                String hash = hashCalculator.calculateSHA256(
                        task.getFilePath()
                );

                // Check if already backed up
                boolean duplicate = deduplicationEngine.isDuplicate(
                        hash,
                        task.getFilePath()
                );

                if (duplicate) {

                    System.out.printf(
                            "[%s] Duplicate Skipped : %s%n",
                            workerName,
                            task.getFilePath().getFileName()
                    );
                    statistics.duplicateSkipped();
                    return;
                }

                // Compress file
                Path backupLocation = compressionManager.compress(
                        task.getFilePath(),
                        hash
                );

                // Create metadata
                FileMetadata metadata =
                        new FileMetadata(

                                task.getFilePath().toString(),

                                hash,

                                backupLocation.toString(),

                                task.getSize(),

                                Files.size(backupLocation),

                                LocalDateTime.now().toString(),

                                Files.getLastModifiedTime(
                                                task.getFilePath())
                                        .toMillis(),

                                false
                        );

                // Send metadata to metadata writer
                metadataQueue.put(metadata);

                statistics.fileBackedUp(

                        task.getSize(),

                        Files.size(backupLocation)

                );
                System.out.printf(
                        "[%s] Backed Up : %s -> %s%n",
                        workerName,
                        task.getFilePath().getFileName(),
                        backupLocation.getFileName()
                );

            } catch (Exception e) {
                Thread.sleep(
                        (long) Math.pow(2, attempts) * 500
                );
                attempts++;

                System.err.printf(

                        "[%s] Retry %d/%d : %s%n",

                        workerName,

                        attempts,

                        MAX_RETRIES,

                        task.getFilePath().getFileName()

                );

                if (attempts == MAX_RETRIES) {
                    statistics.fileFailed();
                    System.out.println(

                            "Failed Files        : "

                                    + statistics.getFailedFiles()

                    );

                    System.err.printf(

                            "[%s] FAILED : %s%n",

                            workerName,

                            task.getFilePath()

                    );



                }

            }

        }
    }
}