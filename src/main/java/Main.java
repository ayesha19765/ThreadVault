//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

import backup.BackupManager;
import restore.RestoreManager;

public class Main {

    public static void main(String[] args) {

        BackupManager manager =
                new BackupManager();

        manager.startBackup("sample_data");

        RestoreManager restoreManager =
                new RestoreManager();

        restoreManager.restoreAll();
    }
}
