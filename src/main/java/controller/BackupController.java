package controller;

import dto.request.BackupRequest;
import dto.request.RestoreRequest;
import dto.response.BackupJobResponse;
import dto.response.RestoreResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.BackupService;

import java.util.List;

/**
 * REST controller exposing ThreadVault backup and restore endpoints.
 */
@RestController
@RequestMapping("/api/backups")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    /**
     * Initiates a new backup operation asynchronously.
     *
     * @param request backup configuration parameters
     * @return 202 Accepted with the created backup job response
     */
    @PostMapping
    public ResponseEntity<BackupJobResponse> startBackup(@Valid @RequestBody BackupRequest request) {
        BackupJobResponse response = backupService.submitBackup(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Retrieves the status and progress of a specific backup job.
     *
     * @param id backup job identifier
     * @return 200 OK with backup job details
     */
    @GetMapping("/{id}")
    public ResponseEntity<BackupJobResponse> getBackupById(@PathVariable("id") String id) {
        BackupJobResponse response = backupService.getBackupJob(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the list of all recent backup jobs.
     *
     * @return 200 OK with list of backup jobs
     */
    @GetMapping
    public ResponseEntity<List<BackupJobResponse>> getAllBackups() {
        List<BackupJobResponse> responses = backupService.getAllBackupJobs();
        return ResponseEntity.ok(responses);
    }

    /**
     * Triggers restore for a specific backup job or all catalog files.
     *
     * @param id backup job identifier
     * @param request optional restore settings
     * @return 200 OK with restore result summary
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<RestoreResponse> restoreBackup(
            @PathVariable("id") String id,
            @RequestBody(required = false) RestoreRequest request
    ) {
        RestoreResponse response = backupService.restoreBackup(id, request != null ? request : new RestoreRequest());
        return ResponseEntity.ok(response);
    }
}
