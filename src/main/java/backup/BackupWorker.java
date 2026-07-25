package backup;

import scanner.FileTask;

import java.util.concurrent.BlockingQueue;

public class BackupWorker implements Runnable {

    private final BlockingQueue<FileTask> queue;
    private final int workerId;

    public BackupWorker(int workerId,
                        BlockingQueue<FileTask> queue) {

        this.workerId = workerId;
        this.queue = queue;
    }

    @Override
    public void run() {

        try {

            while (true) {

                FileTask task = queue.take();

                // Poison Pill
                if (task == FileTask.POISON_PILL) {
                    break;
                }

                System.out.printf(
                        "[Worker-%d] Processing %s (%d bytes)%n",
                        workerId,
                        task.getFilePath().getFileName(),
                        task.getSize());

                // simulate backup

                Thread.sleep(500);

            }

            System.out.printf(
                    "[Worker-%d] Stopped%n",
                    workerId);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

        }

    }

}