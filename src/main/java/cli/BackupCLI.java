package cli;

import backup.BackupManager;
import restore.RestoreManager;

import java.util.Scanner;

public class BackupCLI {

    private static final int BACKUP = 1;
    private static final int RESTORE = 2;
    private static final int EXIT = 3;

    private final BackupManager backupManager;

    private final RestoreManager restoreManager;

    private final Scanner scanner =
            new Scanner(System.in);

    public BackupCLI(
            BackupManager backupManager,
            RestoreManager restoreManager) {

        this.backupManager = backupManager;
        this.restoreManager = restoreManager;

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

                        backupManager.startBackup(
                                backupDirectory);

                    } catch (Exception e) {

                        System.err.println(
                                "Backup failed: "
                                        + e.getMessage());

                    }

                }

                case RESTORE -> {

                    try {

                        restoreManager.restoreAll();

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