import backup.BackupManager;
import cli.BackupCLI;
import config.AppConfig;
import restore.RestoreManager;

public class Main {

    public static void main(String[] args) {

        AppConfig config =
                new AppConfig();

        BackupManager backupManager =
                new BackupManager();

        RestoreManager restoreManager =
                new RestoreManager();

        BackupCLI cli =
                new BackupCLI(
                        backupManager,
                        restoreManager);

        cli.start(
                config.getBackupDirectory());

    }

}