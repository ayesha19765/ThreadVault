import backup.BackupManager;
import cli.BackupCLI;
import config.AppConfig;
import restore.RestoreManager;

/**
 * Application entry point for ThreadVault.
 *
 * <p>This class acts as the composition root where
 * application components are created and connected.</p>
 *
 * <p>Main is responsible only for bootstrapping the application.
 * Business logic belongs inside the respective services.</p>
 */
public class Main {

    /**
     * Starts the ThreadVault backup application.
     *
     * @param args command-line arguments
     */
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