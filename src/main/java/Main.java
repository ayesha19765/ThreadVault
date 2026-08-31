import backup.BackupManager;
import cli.BackupCLI;
import config.AppConfig;
import org.springframework.boot.SpringApplication;
import restore.RestoreManager;
import service.BackupJobRegistry;
import service.BackupService;
import service.BackupServiceImpl;

import java.util.concurrent.Executors;

/**
 * Application entry point for ThreadVault.
 *
 * <p>Supports starting in interactive CLI mode or starting the Spring Boot
 * web server when passed {@code --server}.</p>
 */
public class Main {

    /**
     * Starts the ThreadVault application.
     *
     * @param args command-line arguments (pass {@code --server} to start Spring Boot web server)
     */
    public static void main(String[] args) {

        if (args != null && args.length > 0 && "--server".equalsIgnoreCase(args[0])) {
            SpringApplication.run(ThreadVaultApplication.class, args);
            return;
        }

        AppConfig config =
                new AppConfig();

        BackupManager backupManager =
                new BackupManager();

        RestoreManager restoreManager =
                new RestoreManager();

        BackupService backupService =
                new BackupServiceImpl(
                        new BackupJobRegistry(),
                        backupManager,
                        restoreManager,
                        config.getWorkerCount(),
                        Executors.newCachedThreadPool()
                );

        BackupCLI cli =
                new BackupCLI(backupService);

        cli.start(
                config.getBackupDirectory());

    }

}
