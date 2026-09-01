package incremental;

import backup.BackupManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import stats.BackupStatistics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class IncrementalBackupCorrectnessTest {

    @TempDir
    Path tempDir;

    private Path sourceDir;
    private BackupManager backupManager;

    @BeforeEach
    void setUp() throws IOException {
        sourceDir = tempDir.resolve("test_dataset");
        Files.createDirectories(sourceDir);
        backupManager = new BackupManager();
    }

    @Test
    void testIncrementalLifecycleRuns() throws Exception {
        // --- Run 1: Initial Backup ---
        Path docDir = sourceDir.resolve("documents");
        Files.createDirectories(docDir);
        Path file1 = docDir.resolve("report.txt");
        Files.writeString(file1, "Initial report content version 1.0");

        Path binDir = sourceDir.resolve("data");
        Files.createDirectories(binDir);
        Path file2 = binDir.resolve("sample.bin");
        byte[] binaryData = new byte[] {0x10, 0x20, 0x30, 0x40, 0x50};
        Files.write(file2, binaryData);

        BackupStatistics run1Stats = backupManager.startBackup(sourceDir.toString(), 2);
        assertEquals(2, run1Stats.getFilesScanned(), "Run 1: Expected 2 files scanned");
        assertEquals(2, run1Stats.getFilesBackedUp(), "Run 1: Expected 2 files backed up");
        assertEquals(0, run1Stats.getIncrementalSkipped(), "Run 1: Expected 0 skipped");

        // --- Run 2: Unchanged Backup ---
        BackupStatistics run2Stats = backupManager.startBackup(sourceDir.toString(), 2);
        assertEquals(2, run2Stats.getFilesScanned(), "Run 2: Expected 2 files scanned");
        assertEquals(0, run2Stats.getFilesBackedUp(), "Run 2: Expected 0 files backed up");
        assertEquals(2, run2Stats.getIncrementalSkipped(), "Run 2: Expected all 2 files skipped");

        // --- Run 3: Modify One File ---
        Thread.sleep(100); // Ensure timestamp difference
        Files.writeString(file1, "Updated report content version 2.0 - modified");
        BackupStatistics run3Stats = backupManager.startBackup(sourceDir.toString(), 2);
        assertEquals(2, run3Stats.getFilesScanned(), "Run 3: Expected 2 files scanned");
        assertEquals(1, run3Stats.getFilesBackedUp(), "Run 3: Expected modified file backed up");
        assertEquals(1, run3Stats.getIncrementalSkipped(), "Run 3: Expected unchanged binary skipped");

        // --- Run 4: Add New File ---
        Path nestedDir = sourceDir.resolve("nested");
        Files.createDirectories(nestedDir);
        Path file3 = nestedDir.resolve("new_file.txt");
        Files.writeString(file3, "New nested file content");

        BackupStatistics run4Stats = backupManager.startBackup(sourceDir.toString(), 2);
        assertEquals(3, run4Stats.getFilesScanned(), "Run 4: Expected 3 files scanned");
        assertEquals(1, run4Stats.getFilesBackedUp(), "Run 4: Expected new file backed up");
        assertEquals(2, run4Stats.getIncrementalSkipped(), "Run 4: Expected 2 unchanged files skipped");

        // --- Run 5: Duplicate Content File ---
        Path file4 = docDir.resolve("report-copy.txt");
        Files.writeString(file4, "Updated report content version 2.0 - modified"); // identical to file1

        BackupStatistics run5Stats = backupManager.startBackup(sourceDir.toString(), 2);
        assertEquals(4, run5Stats.getFilesScanned(), "Run 5: Expected 4 files scanned");
        assertEquals(1, run5Stats.getDuplicatesSkipped(), "Run 5: Expected 1 duplicate skipped");
        assertEquals(3, run5Stats.getIncrementalSkipped(), "Run 5: Expected 3 unchanged files skipped");
    }
}

