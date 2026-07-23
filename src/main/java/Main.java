//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

import backup.BackupManager;

public class Main {

    public static void main(String[] args) {

        BackupManager backupManager = new BackupManager();

        backupManager.startBackup("sample_data");
    }
}
