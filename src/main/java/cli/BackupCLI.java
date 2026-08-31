package cli;

import backup.BackupManager;
import restore.RestoreManager;
import service.BackupJobRegistry;
import service.BackupService;
import service.BackupServiceImpl;

import java.util.Scanner;
import java.util.concurrent.Executors;

/**
 * Command-line interface for ThreadVault.
 *
 * <p>Routes all operations through the Application Service Layer
 * to maintain architectural consistency with the REST API.</p>
 */
public class BackupCLI {

    private static final int BACKUP = 1;
    private static final int RESTORE = 2;
    private static final int EXIT = 3;

    private final BackupService backupService;

    private final Scanner scanner =
            new Scanner(System.in);

    public BackupCLI(BackupService backupService) {
        this.backupService = backupService;
    }

    public BackupCLI(
            BackupManager backupManager,
            RestoreManager restoreManager) {
        this(new BackupServiceImpl(
                new BackupJobRegistry(),
                backupManager,
                restoreManager,
                4,
                Executors.newCachedThreadPool()
        ));
    }

    /**
     * Starts the command-line interface.
     */
    public void start(String backupDirectory) {

        while (true) {

            printMenu();

            int choice = readChoice();

            switch (choice) {

                case BACKUP -> {

                    try {

                        backupService.executeBackup(
                                backupDirectory,
                                0);

                    } catch (Exception e) {

                        System.err.println(
                                "Backup failed: "
                                        + e.getMessage());

                    }

                }

                case RESTORE -> {

                    try {

                        backupService.restoreAll();

                    } catch (Exception e) {

                        System.err.println(
                                "Restore failed: "
                                        + e.getMessage());

                    }

                }

                case EXIT -> {

                    System.out.println(
                            "Goodbye!");

                    return;

                }

                default -> System.out.println(
                        "Invalid choice. Try again.");

            }

        }

    }

    private void printMenu() {

        System.out.println();

        System.out.println(
                "========= ThreadVault Backup Engine =========");

        System.out.println("1. Backup Now");
        System.out.println("2. Restore Files");
        System.out.println("3. Exit");

        System.out.print("Choice : ");

    }

    private int readChoice() {

        try {

            return Integer.parseInt(
                    scanner.nextLine());

        } catch (NumberFormatException e) {

            return -1;

        }

    }

}
