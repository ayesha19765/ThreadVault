package backup;

import dedup.DeduplicationEngine;
import dedup.HashCalculator;
import scanner.FileTask;

import java.util.concurrent.BlockingQueue;

public class BackupWorker implements Runnable {

    private final BlockingQueue<FileTask> queue;
    private final DeduplicationEngine dedupEngine;
    private final HashCalculator hashCalculator;

    public BackupWorker(
            BlockingQueue<FileTask> queue,
            DeduplicationEngine dedupEngine) {

        this.queue = queue;
        this.dedupEngine = dedupEngine;
        this.hashCalculator = new HashCalculator();
    }

    @Override
    public void run() {

        String workerName = Thread.currentThread().getName();

        try {

            while (true) {

                FileTask task = queue.take();

                // Stop signal
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

            String hash =
                    hashCalculator.calculateSHA256(
                            task.getFilePath());

            boolean duplicate =
                    dedupEngine.isDuplicate(
                            hash,
                            task.getFilePath());

            if (duplicate) {

                System.out.printf(
                        "[%s] Duplicate Skipped : %s%n",
                        workerName,
                        task.getFilePath().getFileName());

            } else {

                System.out.printf(
                        "[%s] Backing Up : %s%n",
                        workerName,
                        task.getFilePath().getFileName());

                // Simulate backup work
                Thread.sleep(300);

            }

        } catch (Exception e) {

            System.err.printf(
                    "[%s] Error processing %s : %s%n",
                    workerName,
                    task.getFilePath(),
                    e.getMessage());

        }

    }

}