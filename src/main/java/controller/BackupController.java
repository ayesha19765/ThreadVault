package controller;

import dto.request.BackupRequest;
import dto.request.RestoreRequest;
import dto.response.BackupJobResponse;
import dto.response.RestoreResponse;
import event.BackupEvent;
import event.BackupEventPublisher;
import event.BackupEventType;
import model.BackupStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import service.BackupService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * REST controller exposing ThreadVault backup, restore, and real-time SSE progress endpoints.
 */
@RestController
@RequestMapping("/api/backups")
public class BackupController {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes

    private final BackupService backupService;
    private final BackupEventPublisher eventPublisher;

    public BackupController(BackupService backupService, BackupEventPublisher eventPublisher) {
        this.backupService = backupService;
        this.eventPublisher = eventPublisher;
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
     * Streams real-time Server-Sent Events (SSE) for an active or completed backup job.
     *
     * @param id backup job identifier
     * @return SseEmitter streaming backup progress
     */
    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBackupProgress(@PathVariable("id") String id) {
        // Validate job exists (throws BackupNotFoundException if absent)
        BackupJobResponse initialJob = backupService.getBackupJob(id);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        // If job is already finished, emit current state and complete immediately
        if (initialJob.getStatus() == BackupStatus.COMPLETED || initialJob.getStatus() == BackupStatus.FAILED) {
            try {
                emitter.send(SseEmitter.event()
                        .id(UUID.randomUUID().toString())
                        .name(initialJob.getStatus() == BackupStatus.COMPLETED
                                ? BackupEventType.BACKUP_COMPLETED.name()
                                : BackupEventType.BACKUP_FAILED.name())
                        .data(initialJob));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // Define thread-safe event listener for active backup
        final Consumer<BackupEvent> listener = new Consumer<>() {
            @Override
            public void accept(BackupEvent event) {
                try {
                    emitter.send(SseEmitter.event()
                            .id(UUID.randomUUID().toString())
                            .name(event.getType().name())
                            .data(event));

                    if (event.getType() == BackupEventType.BACKUP_COMPLETED || event.getType() == BackupEventType.BACKUP_FAILED) {
                        eventPublisher.unsubscribe(id, this);
                        emitter.complete();
                    }
                } catch (Exception ex) {
                    eventPublisher.unsubscribe(id, this);
                    emitter.completeWithError(ex);
                }
            }
        };

        // Clean up subscriber references on completion, timeout, or client disconnect
        emitter.onCompletion(() -> eventPublisher.unsubscribe(id, listener));
        emitter.onTimeout(() -> {
            eventPublisher.unsubscribe(id, listener);
            emitter.complete();
        });
        emitter.onError(e -> eventPublisher.unsubscribe(id, listener));

        // Subscribe to event hub
        eventPublisher.subscribe(id, listener);

        // Send initial connection handshake with current job snapshot
        try {
            emitter.send(SseEmitter.event()
                    .name("INITIAL_STATE")
                    .data(initialJob));
        } catch (IOException e) {
            eventPublisher.unsubscribe(id, listener);
            emitter.completeWithError(e);
        }

        return emitter;
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
