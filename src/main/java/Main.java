//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

import backup.BackupManager;
import restore.RestoreManager;
import scheduler.BackupScheduler;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        BackupManager manager =
                new BackupManager();

        BackupScheduler scheduler =
                new BackupScheduler();

        scheduler.start(

                manager,

                "sample_data",

                30

        );

        Thread.currentThread().join();

        RestoreManager restoreManager =
                new RestoreManager();

        restoreManager.restoreAll();
    }
}
