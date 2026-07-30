package backup;

import compression.CompressionManager;
import dedup.DeduplicationEngine;
import dedup.HashCalculator;
import metadata.FileMetadata;
import metadata.MetadataStore;
import scanner.FileTask;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;

public class BackupWorker implements Runnable {

    private final BlockingQueue<FileTask> queue;

    private final DeduplicationEngine deduplicationEngine;

    private final CompressionManager compressionManager;

    private final MetadataStore metadataStore;

    private final HashCalculator hashCalculator;

    public BackupWorker(

            BlockingQueue<FileTask> queue,

            DeduplicationEngine deduplicationEngine,

            CompressionManager compressionManager,

            MetadataStore metadataStore

    ) {

        this.queue = queue;
        this.deduplicationEngine = deduplicationEngine;
        this.compressionManager = compressionManager;
        this.metadataStore = metadataStore;
        this.hashCalculator = new HashCalculator();

    }

    @Override
    public void run() {

        String workerName = Thread.currentThread().getName();

        try {

            while (true) {

                FileTask task = queue.take();

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

            // Calculate SHA-256 Hash
            String hash =
                    hashCalculator.calculateSHA256(
                            task.getFilePath());

            // Check Duplicate
            boolean duplicate =
                    deduplicationEngine.isDuplicate(
                            hash,
                            task.getFilePath());

            if (duplicate) {

                System.out.printf(
                        "[%s] Duplicate Skipped : %s%n",
                        workerName,
                        task.getFilePath().getFileName());

                return;

            }

            // Compress File
            Path backupLocation =
                    compressionManager.compress(
                            task.getFilePath(),
                            hash);

            // Create Metadata
            FileMetadata metadata =
                    new FileMetadata(

                            task.getFilePath().toString(),

                            hash,

                            backupLocation.toString(),

                            task.getSize(),

                            Files.size(backupLocation),

                            LocalDateTime.now().toString()

                    );

            // Save Metadata
            metadataStore.save(metadata);

            System.out.printf(
                    "[%s] Backed Up : %s -> %s%n",
                    workerName,
                    task.getFilePath().getFileName(),
                    backupLocation.getFileName());

        }

        catch (Exception e) {

            System.err.printf(
                    "[%s] Failed : %s%n",
                    workerName,
                    task.getFilePath());

            e.printStackTrace();

        }

    }

}