package backup;

import scanner.FileTask;

import java.util.concurrent.BlockingQueue;

public class BackupWorker implements Runnable {

    private final BlockingQueue<FileTask> queue;

    public BackupWorker(BlockingQueue<FileTask> queue) {
        this.queue = queue;
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

                System.out.printf(
                        "%s processing %s (%d bytes)%n",
                        workerName,
                        task.getFilePath().getFileName(),
                        task.getSize());

                // Simulate backup work
                Thread.sleep(500);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
