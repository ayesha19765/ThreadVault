package backup;

import scanner.DirectoryScanner;
import scanner.FileTask;

import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class BackupManager {

    private static final int WORKERS = 3;

    public void startBackup(String folder) {

        BlockingQueue<FileTask> queue =
                new ArrayBlockingQueue<>(100);

        Thread[] workers =
                new Thread[WORKERS];

        for (int i = 0; i < WORKERS; i++) {

            workers[i] =
                    new Thread(
                            new BackupWorker(i + 1,
                                    queue));

            workers[i].start();

        }

        DirectoryScanner scanner =
                new DirectoryScanner();

        scanner.scan(Path.of(folder), queue);

        for (int i = 0; i < WORKERS; i++) {

            try {

                queue.put(FileTask.POISON_PILL);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

            }

        }

        for (Thread worker : workers) {

            try {

                worker.join();

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

            }

        }

        System.out.println("\nBackup Complete.");

    }

}