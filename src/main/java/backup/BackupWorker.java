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

public class BackupWorker implements Runnable {

    private final BlockingQueue<FileTask> fileQueue;
    private final BlockingQueue<FileMetadata> metadataQueue;

    private final DeduplicationEngine deduplicationEngine;
    private final CompressionManager compressionManager;
    private final HashCalculator hashCalculator;

    public BackupWorker(
            BlockingQueue<FileTask> fileQueue,
            BlockingQueue<FileMetadata> metadataQueue,
            DeduplicationEngine deduplicationEngine,
            CompressionManager compressionManager
    ) {

        this.fileQueue = fileQueue;
        this.metadataQueue = metadataQueue;
        this.deduplicationEngine = deduplicationEngine;
        this.compressionManager = compressionManager;
        this.hashCalculator = new HashCalculator();
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

    private void processFile(FileTask task, String workerName) {

        try {

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

                return;
            }

            // Compress file
            Path backupLocation = compressionManager.compress(
                    task.getFilePath(),
                    hash
            );

            // Create metadata
            FileMetadata metadata = new FileMetadata(
                    task.getFilePath().toString(),
                    hash,
                    backupLocation.toString(),
                    task.getSize(),
                    Files.size(backupLocation),
                    LocalDateTime.now().toString()
            );

            // Send metadata to metadata writer
            metadataQueue.put(metadata);

            System.out.printf(
                    "[%s] Backed Up : %s -> %s%n",
                    workerName,
                    task.getFilePath().getFileName(),
                    backupLocation.getFileName()
            );

        } catch (Exception e) {

            System.err.printf(
                    "[%s] Failed to process %s%n",
                    workerName,
                    task.getFilePath()
            );

            e.printStackTrace();
        }
    }
}