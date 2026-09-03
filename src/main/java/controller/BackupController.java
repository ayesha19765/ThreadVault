package controller;

import dto.request.BackupRequest;
import dto.request.RestoreRequest;
import dto.response.BackupJobResponse;
import dto.response.ErrorResponse;
import dto.response.RestoreResponse;
import event.BackupEvent;
import event.BackupEventPublisher;
import event.BackupEventType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import model.BackupStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Tag(name = "Backup Operations", description = "Endpoints for initiating backups, inspecting live status, streaming events, and restoring archives")
public class BackupController {

    private static final Logger logger = LoggerFactory.getLogger(BackupController.class);
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
    @Operation(summary = "Start Backup Job", description = "Submits a new backup job for asynchronous execution using worker thread pools.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Backup job accepted and queued for execution",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BackupJobResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or non-existent source directory",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BackupJobResponse> startBackup(@Valid @RequestBody BackupRequest request) {
        logger.info("Received backup request for source: {}, workers: {}", request.getSource(), request.getWorkers());
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
    @Operation(summary = "Get Backup Status", description = "Retrieves the execution status, metrics, and progress of a specific backup job.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Backup job details retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BackupJobResponse.class))),
            @ApiResponse(responseCode = "404", description = "Backup job ID not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BackupJobResponse> getBackupById(
            @Parameter(description = "Unique UUID of the backup job", required = true)
            @PathVariable("id") String id
    ) {
        BackupJobResponse response = backupService.getBackupJob(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the list of all recent backup jobs.
     *
     * @return 200 OK with list of backup jobs
     */
    @GetMapping
    @Operation(summary = "List All Backups", description = "Retrieves all active, completed, and failed backup jobs sorted by creation timestamp descending.")
    @ApiResponse(responseCode = "200", description = "List of backup jobs",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = BackupJobResponse.class))))
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
    @Operation(summary = "Stream Backup Progress (SSE)", description = "Establishes a real-time Server-Sent Events (SSE) stream delivering live backup events.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Real-time event stream connected",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)),
            @ApiResponse(responseCode = "404", description = "Backup job ID not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public SseEmitter streamBackupProgress(
            @Parameter(description = "Unique UUID of the backup job", required = true)
            @PathVariable("id") String id
    ) {
        BackupJobResponse initialJob = backupService.getBackupJob(id);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

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

        emitter.onCompletion(() -> eventPublisher.unsubscribe(id, listener));
        emitter.onTimeout(() -> {
            eventPublisher.unsubscribe(id, listener);
            emitter.complete();
        });
        emitter.onError(e -> eventPublisher.unsubscribe(id, listener));

        eventPublisher.subscribe(id, listener);

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
    @Operation(summary = "Restore Backup", description = "Reconstructs backed-up files from compressed archives and verifies directory structure.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files restored successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestoreResponse.class))),
            @ApiResponse(responseCode = "404", description = "Backup ID not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Restore error or path traversal rejection",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RestoreResponse> restoreBackup(
            @Parameter(description = "Unique UUID of the backup job", required = true)
            @PathVariable("id") String id,
            @RequestBody(required = false) RestoreRequest request
    ) {
        logger.info("Restore requested for backupId: {}", id);
        RestoreResponse response = backupService.restoreBackup(id, request != null ? request : new RestoreRequest());
        return ResponseEntity.ok(response);
    }
}
