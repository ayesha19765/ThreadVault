package watcher;

import backup.BackupManager;

import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

public class DirectoryWatcher {

    private final WatchService watchService;

    private final BackupManager backupManager;

    private final Path directory;

    public DirectoryWatcher(

            String directory,

            BackupManager backupManager

    ) throws IOException {

        this.directory = Path.of(directory);

        this.backupManager = backupManager;

        watchService =
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
                "Watching : " + directory);

        while (true) {

            try {

                WatchKey key =
                        watchService.take();

                for (WatchEvent<?> event :
                        key.pollEvents()) {

                    WatchEvent.Kind<?> kind =
                            event.kind();

                    Path changedFile =
                            directory.resolve(
                                    (Path) event.context());

                    System.out.printf(

                            "Detected %s : %s%n",

                            kind.name(),

                            changedFile.getFileName()

                    );

                    if (kind != ENTRY_DELETE) {

                        backupManager.startBackup(
                                directory.toString());

                    }

                }

                key.reset();

            }

            catch (Exception e) {

                e.printStackTrace();

            }

        }

    }

}