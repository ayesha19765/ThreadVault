package service;

import backup.BackupManager;
import dto.request.BackupRequest;
import dto.request.RestoreRequest;
import dto.response.BackupJobResponse;
import dto.response.RestoreResponse;
import event.BackupEventHub;
import exception.BackupNotFoundException;
import model.BackupStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import restore.RestoreManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class BackupServiceTest {

    @TempDir
    Path tempDir;

    private BackupJobRegistry jobRegistry;
    private BackupEventHub eventHub;
    private BackupService backupService;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        jobRegistry = new BackupJobRegistry();
        eventHub = new BackupEventHub();
        executorService = Executors.newCachedThreadPool();
        backupService = new BackupServiceImpl(
                jobRegistry,
                new BackupManager(),
                new RestoreManager(),
                eventHub,
                2,
                executorService
        );
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void testSubmitBackup_CreatesJobAndStartsAsynchronously() throws IOException, InterruptedException {
        Path testFile1 = tempDir.resolve("test-file-1.txt");
        Path testFile2 = tempDir.resolve("test-file-2.txt");
        Files.writeString(testFile1, "Hello from test file 1");
        Files.writeString(testFile2, "Hello from test file 2");

        BackupRequest request = new BackupRequest(tempDir.toString(), "backup_storage", 2);
        BackupJobResponse response = backupService.submitBackup(request);

        assertNotNull(response);
        assertNotNull(response.getBackupId());
        assertEquals(BackupStatus.QUEUED, response.getStatus());
        assertEquals(tempDir.toString(), response.getSource());
        assertEquals(2, response.getWorkers());

        // Wait for async background completion
        int attempts = 0;
        BackupJobResponse status = null;
        while (attempts < 20) {
            Thread.sleep(200);
            status = backupService.getBackupJob(response.getBackupId());
            if (status.getStatus() == BackupStatus.COMPLETED || status.getStatus() == BackupStatus.FAILED) {
                break;
            }
            attempts++;
        }

        assertNotNull(status);
        assertEquals(BackupStatus.COMPLETED, status.getStatus());
        assertEquals(2, status.getFilesDiscovered());
        assertEquals(2, status.getFilesProcessed());
    }

    @Test
    void testSubmitBackup_InvalidSource_ThrowsIllegalArgumentException() {
        BackupRequest request = new BackupRequest("/non/existent/path/xyz", "backup_storage", 2);
        assertThrows(IllegalArgumentException.class, () -> backupService.submitBackup(request));
    }

    @Test
    void testSubmitBackup_BlankSource_ThrowsIllegalArgumentException() {
        BackupRequest request = new BackupRequest("   ", "backup_storage", 2);
        assertThrows(IllegalArgumentException.class, () -> backupService.submitBackup(request));
    }

    @Test
    void testGetBackupJob_UnknownId_ThrowsBackupNotFoundException() {
        assertThrows(BackupNotFoundException.class, () -> backupService.getBackupJob("unknown-job-id-999"));
    }

    @Test
    void testGetAllBackupJobs_ReturnsList() throws IOException {
        Path testFile = tempDir.resolve("single.txt");
        Files.writeString(testFile, "Single file content");

        BackupRequest request = new BackupRequest(tempDir.toString(), "backup_storage", 1);
        backupService.submitBackup(request);

        List<BackupJobResponse> all = backupService.getAllBackupJobs();
        assertNotNull(all);
        assertFalse(all.isEmpty());
    }

    @Test
    void testRestoreBackup_Success() {
        RestoreResponse response = backupService.restoreBackup(null, new RestoreRequest());
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void testRestoreBackup_UnknownId_ThrowsBackupNotFoundException() {
        assertThrows(BackupNotFoundException.class, () ->
                backupService.restoreBackup("non-existent-id", new RestoreRequest()));
    }
}

