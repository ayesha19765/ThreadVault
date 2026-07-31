package watcher;

import backup.BackupManager;

import java.io.IOException;
import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Watches a directory for filesystem changes
 * and triggers incremental backups.
 *
 * Uses Java WatchService to provide event-driven
 * backup execution instead of continuous polling.
 */
public class DirectoryWatcher {

    private final WatchService watchService;

    private final BackupManager backupManager;

    private final Path directory;

    private volatile boolean running = true;


    public DirectoryWatcher(
            Path directory,
            BackupManager backupManager
    ) throws IOException {

        this.directory = directory;

        this.backupManager = backupManager;

        this.watchService =
                FileSystems.getDefault()
                        .newWatchService();


        this.directory.register(
                watchService,
                ENTRY_CREATE,
                ENTRY_MODIFY,
                ENTRY_DELETE
        );

    }


    public void start() {

        System.out.println(
                "[Watcher] Watching : "
                        + directory
        );


        while (running) {

            try {

                WatchKey key =
                        watchService.take();


                for (WatchEvent<?> event :
                        key.pollEvents()) {


                    WatchEvent.Kind<?> kind =
                            event.kind();


                    if (kind == OVERFLOW) {

                        continue;

                    }


                    Path changedFile =
                            directory.resolve(
                                    (Path) event.context()
                            );


                    System.out.printf(
                            "[Watcher] Detected %s : %s%n",
                            kind.name(),
                            changedFile.getFileName()
                    );


                    if (kind != ENTRY_DELETE) {

                        backupManager.startBackup(
                                directory.toString()
                        );

                    }

                }


                boolean valid =
                        key.reset();


                if (!valid) {

                    break;

                }


            } catch (InterruptedException e) {

                Thread.currentThread()
                        .interrupt();

                break;


            } catch (ClosedWatchServiceException e) {

                break;


            } catch (Exception e) {

                System.err.println(
                        "[Watcher] Error while watching directory."
                );

                e.printStackTrace();

            }

        }

    }


    public void stop() {

        running = false;

        try {

            watchService.close();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to stop directory watcher.",
                    e
            );

        }

    }

}