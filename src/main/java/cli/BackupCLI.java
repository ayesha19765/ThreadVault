package cli;

import backup.BackupManager;
import restore.RestoreManager;

import java.util.Scanner;

public class BackupCLI {

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

    public void start(String backupDirectory) {

        while (true) {

            System.out.println();
            System.out.println(
                    "========= Mini Backup Engine =========");

            System.out.println("1. Backup Now");
            System.out.println("2. Restore Files");
            System.out.println("3. Exit");

            System.out.print("Choice : ");

            int choice =
                    Integer.parseInt(scanner.nextLine());

            switch (choice) {

                case 1 -> backupManager.startBackup(
                        backupDirectory);

                case 2 -> restoreManager.restoreAll();

                case 3 -> {

                    System.out.println(
                            "Goodbye!");

                    return;

                }

                default -> System.out.println(
                        "Invalid Choice");

            }

        }

    }

}