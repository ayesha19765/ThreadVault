package backup;

import scanner.DirectoryScanner;
import scanner.FileTask;

import java.nio.file.Path;
import java.util.List;

public class BackupManager {

    private final DirectoryScanner scanner;

    public BackupManager() {
        this.scanner = new DirectoryScanner();
    }

    public void startBackup(String folderPath) {

        System.out.println("==================================");
        System.out.println(" Mini Backup Engine");
        System.out.println("==================================");

        System.out.println("\nScanning: " + folderPath);

        List<FileTask> files = scanner.scan(Path.of(folderPath));

        long totalSize = 0;

        for (FileTask file : files) {

            totalSize += file.getSize();

            System.out.printf("✓ %s (%s)%n",
                    file.getFilePath(),
                    humanReadable(file.getSize()));
        }

        System.out.println("----------------------------------");
        System.out.println("Files Found : " + files.size());
        System.out.println("Total Size  : " + humanReadable(totalSize));
        System.out.println("----------------------------------");
    }

    private String humanReadable(long bytes) {

        double size = bytes;

        String[] units = {"B", "KB", "MB", "GB", "TB"};

        int unit = 0;

        while (size >= 1024 && unit < units.length - 1) {
            size /= 1024;
            unit++;
        }

        return String.format("%.2f %s", size, units[unit]);
    }
}