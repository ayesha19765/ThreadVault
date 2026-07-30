//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

import backup.BackupManager;
import restore.RestoreManager;
import scheduler.BackupScheduler;
import watcher.DirectoryWatcher;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws InterruptedException, IOException {


        BackupManager manager =
                new BackupManager();

        DirectoryWatcher watcher =
                new DirectoryWatcher(

                        "sample_data",

                        manager

                );

        watcher.start();

        Thread.currentThread().join();

        RestoreManager restoreManager =
                new RestoreManager();

        restoreManager.restoreAll();
    }
}
