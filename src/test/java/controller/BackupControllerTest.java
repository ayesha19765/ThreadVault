package controller;

import dto.request.BackupRequest;
import dto.request.RestoreRequest;
import dto.response.BackupJobResponse;
import dto.response.RestoreResponse;
import event.BackupEventHub;
import event.BackupEventPublisher;
import exception.BackupNotFoundException;
import model.BackupStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import service.BackupService;
import stats.BackupStatistics;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class BackupControllerTest {

    private MockMvc mockMvc;
    private BackupEventPublisher eventPublisher;

    private static class StubBackupService implements BackupService {

        @Override
        public BackupJobResponse submitBackup(BackupRequest request) {
            BackupJobResponse response = new BackupJobResponse();
            response.setBackupId("test-id-123");
            response.setStatus(BackupStatus.QUEUED);
            response.setSource(request.getSource());
            response.setDestination(request.getDestination() != null ? request.getDestination() : "backup_storage");
            response.setWorkers(request.getWorkers() != null ? request.getWorkers() : 4);
            response.setCreatedAt(LocalDateTime.now());
            return response;
        }

        @Override
        public BackupStatistics executeBackup(String folderPath, int workerCount) {
            return new BackupStatistics();
        }

        @Override
        public BackupJobResponse getBackupJob(String id) {
            if ("test-id-123".equals(id)) {
                BackupJobResponse response = new BackupJobResponse();
                response.setBackupId(id);
                response.setStatus(BackupStatus.COMPLETED);
                response.setSource("sample_data");
                response.setDestination("backup_storage");
                response.setWorkers(4);
                response.setFilesDiscovered(5);
                response.setFilesProcessed(5);
                response.setOriginalBytes(1000L);
                response.setStoredBytes(400L);
                response.setSpaceSavedPercentage(60.0);
                response.setCreatedAt(LocalDateTime.now());
                response.setStartedAt(LocalDateTime.now().minusSeconds(5));
                response.setCompletedAt(LocalDateTime.now());
                response.setDurationMs(5000L);
                return response;
            } else if ("active-job-id".equals(id)) {
                BackupJobResponse response = new BackupJobResponse();
                response.setBackupId(id);
                response.setStatus(BackupStatus.RUNNING);
                response.setSource("sample_data");
                response.setCreatedAt(LocalDateTime.now());
                return response;
            }
            throw new BackupNotFoundException("Backup not found with ID: " + id);
        }

        @Override
        public List<BackupJobResponse> getAllBackupJobs() {
            return List.of(getBackupJob("test-id-123"));
        }

        @Override
        public RestoreResponse restoreBackup(String backupId, RestoreRequest request) {
            if ("invalid-id".equals(backupId)) {
                throw new BackupNotFoundException("Backup not found with ID: " + backupId);
            }
            return new RestoreResponse("SUCCESS", "Files restored successfully", 5);
        }

        @Override
        public int restoreAll() {
            return 5;
        }
    }

    @BeforeEach
    void setUp() {
        BackupService stubService = new StubBackupService();
        this.eventPublisher = new BackupEventHub();
        BackupController controller = new BackupController(stubService, eventPublisher);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testStartBackup_Returns202Accepted() throws Exception {
        String payload = """
                {
                    "source": "sample_data",
                    "destination": "backup_storage",
                    "workers": 4
                }
                """;

        mockMvc.perform(post("/api/backups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.backupId").value("test-id-123"))
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void testStartBackup_BlankSource_Returns400BadRequest() throws Exception {
        String payload = """
                {
                    "source": ""
                }
                """;

        mockMvc.perform(post("/api/backups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));
    }

    @Test
    void testGetBackupById_Success_Returns200OK() throws Exception {
        mockMvc.perform(get("/api/backups/test-id-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backupId").value("test-id-123"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.filesDiscovered").value(5))
                .andExpect(jsonPath("$.filesProcessed").value(5))
                .andExpect(jsonPath("$.spaceSavedPercentage").value(60.0));
    }

    @Test
    void testGetBackupById_NotFound_Returns404NotFound() throws Exception {
        mockMvc.perform(get("/api/backups/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void testGetAllBackups_Returns200OK() throws Exception {
        mockMvc.perform(get("/api/backups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].backupId").value("test-id-123"));
    }

    @Test
    void testStreamBackupProgress_CompletedJob_ReturnsTerminalEventAndCompletes() throws Exception {
        mockMvc.perform(get("/api/backups/test-id-123/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM_VALUE));
    }

    @Test
    void testStreamBackupProgress_ActiveJob_ConnectsAndSubscribes() throws Exception {
        mockMvc.perform(get("/api/backups/active-job-id/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM_VALUE));
    }

    @Test
    void testStreamBackupProgress_UnknownId_Returns404NotFound() throws Exception {
        mockMvc.perform(get("/api/backups/unknown-stream-id/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testRestoreBackup_Success_Returns200OK() throws Exception {
        mockMvc.perform(post("/api/backups/test-id-123/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.restoredFilesCount").value(5));
    }
}
