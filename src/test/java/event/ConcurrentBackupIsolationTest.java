package event;

import backup.BackupManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import stats.BackupStatistics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentBackupIsolationTest {

    @TempDir
    Path tempDir;

    private Path sourceA;
    private Path sourceB;
    private BackupEventHub eventHub;
    private BackupManager backupManager;

    @BeforeEach
    void setUp() throws IOException {
        sourceA = tempDir.resolve("sourceA");
        sourceB = tempDir.resolve("sourceB");
        Files.createDirectories(sourceA);
        Files.createDirectories(sourceB);

        for (int i = 0; i < 5; i++) {
            Files.writeString(sourceA.resolve("fileA_" + i + ".txt"), "Content A " + i);
            Files.writeString(sourceB.resolve("fileB_" + i + ".txt"), "Content B " + i);
        }

        eventHub = new BackupEventHub();
        backupManager = new BackupManager();
    }

    @Test
    void testConcurrentJobsIsolation() throws Exception {
        String jobAId = "job-A-" + System.currentTimeMillis();
        String jobBId = "job-B-" + System.currentTimeMillis();

        List<BackupEvent> eventsA = new CopyOnWriteArrayList<>();
        List<BackupEvent> eventsB = new CopyOnWriteArrayList<>();

        eventHub.subscribe(jobAId, eventsA::add);
        eventHub.subscribe(jobBId, eventsB::add);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<BackupStatistics> taskA = () -> {
            BackupStatistics stats = new BackupStatistics();
            return backupManager.startBackup(sourceA.toString(), 2, stats, jobAId, eventHub);
        };

        Callable<BackupStatistics> taskB = () -> {
            BackupStatistics stats = new BackupStatistics();
            return backupManager.startBackup(sourceB.toString(), 2, stats, jobBId, eventHub);
        };

        Future<BackupStatistics> futureA = executor.submit(taskA);
        Future<BackupStatistics> futureB = executor.submit(taskB);

        BackupStatistics statsA = futureA.get(30, TimeUnit.SECONDS);
        BackupStatistics statsB = futureB.get(30, TimeUnit.SECONDS);

        executor.shutdown();

        assertNotNull(statsA);
        assertNotNull(statsB);
        assertEquals(5, statsA.getFilesScanned());
        assertEquals(5, statsB.getFilesScanned());

        // Verify event isolation: events in list A must only belong to jobA
        assertFalse(eventsA.isEmpty(), "Expected events for Job A");
        for (BackupEvent event : eventsA) {
            assertEquals(jobAId, event.getBackupId(), "Event in list A had wrong job ID");
        }

        // Verify event isolation: events in list B must only belong to jobB
        assertFalse(eventsB.isEmpty(), "Expected events for Job B");
        for (BackupEvent event : eventsB) {
            assertEquals(jobBId, event.getBackupId(), "Event in list B had wrong job ID");
        }
    }
}

