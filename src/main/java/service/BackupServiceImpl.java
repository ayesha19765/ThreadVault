package service;

import backup.BackupManager;
import dto.request.BackupRequest;
import dto.request.RestoreRequest;
import dto.response.BackupJobResponse;
import dto.response.RestoreResponse;
import event.BackupEvent;
import event.BackupEventPublisher;
import event.BackupEventType;
import exception.BackupNotFoundException;
import jakarta.annotation.PreDestroy;
import model.BackupJob;
import model.BackupStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import restore.RestoreManager;
import stats.BackupStatistics;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of BackupService orchestrating ThreadVault core components and domain events.
 */
@Service
public class BackupServiceImpl implements BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupServiceImpl.class);

    private final BackupJobRegistry jobRegistry;
    private final BackupManager backupManager;
    private final RestoreManager restoreManager;
    private final BackupEventPublisher eventPublisher;
    private final int defaultWorkers;
    private final ExecutorService jobExecutor;

    @Autowired
    public BackupServiceImpl(
            BackupJobRegistry jobRegistry,
            BackupEventPublisher eventPublisher,
            @Value("${threadvault.default-workers:4}") int defaultWorkers
    ) {
        this(jobRegistry, new BackupManager(), new RestoreManager(), eventPublisher, defaultWorkers, Executors.newCachedThreadPool());
    }

    public BackupServiceImpl(
            BackupJobRegistry jobRegistry,
            BackupManager backupManager,
            RestoreManager restoreManager,
            int defaultWorkers,
            ExecutorService jobExecutor
    ) {
        this(jobRegistry, backupManager, restoreManager, new event.BackupEventHub(), defaultWorkers, jobExecutor);
    }

    public BackupServiceImpl(
            BackupJobRegistry jobRegistry,
            BackupManager backupManager,
            RestoreManager restoreManager,
            BackupEventPublisher eventPublisher,
            int defaultWorkers,
            ExecutorService jobExecutor
    ) {
        this.jobRegistry = jobRegistry;
        this.backupManager = backupManager;
        this.restoreManager = restoreManager;
        this.eventPublisher = eventPublisher;
        this.defaultWorkers = defaultWorkers > 0 ? defaultWorkers : 4;
        this.jobExecutor = jobExecutor;
    }

    @Override
    public BackupJobResponse submitBackup(BackupRequest request) {
        validateBackupRequest(request);

        String backupId = UUID.randomUUID().toString();
        int workers = (request.getWorkers() != null && request.getWorkers() > 0)
                ? request.getWorkers()
                : defaultWorkers;

        BackupJob job = new BackupJob(
                backupId,
                request.getSource(),
                request.getDestination() != null ? request.getDestination() : "backup_storage",
                workers
        );

        jobRegistry.register(job);
        logger.info("Submitted backup job [{}] for path: {} with {} workers", backupId, request.getSource(), workers);

        jobExecutor.submit(() -> {
            job.setStatus(BackupStatus.RUNNING);
            job.setStartedAt(LocalDateTime.now());
            try {
                backupManager.startBackup(
                        job.getSourcePath(),
                        job.getWorkerCount(),
                        job.getStatistics(),
                        job.getId(),
                        eventPublisher
                );
                job.setStatus(BackupStatus.COMPLETED);

                double spaceSaved = 0.0;
                if (job.getStatistics().getOriginalBytes() > 0) {
                    double compPct = (job.getStatistics().getCompressedBytes() * 100.0) / job.getStatistics().getOriginalBytes();
                    spaceSaved = Math.max(0.0, Math.round((100.0 - compPct) * 100.0) / 100.0);
                }

                logger.info("Backup job [{}] completed. Files scanned: {}, backed up: {}, deduplicated: {}, space saved: {}%",
                        job.getId(),
                        job.getStatistics().getFilesScanned(),
                        job.getStatistics().getFilesBackedUp(),
                        job.getStatistics().getDuplicatesSkipped(),
                        spaceSaved);

                if (eventPublisher != null) {
                    eventPublisher.publish(
                            BackupEvent.builder(job.getId(), BackupEventType.BACKUP_COMPLETED)
                                     .stats(
                                            job.getStatistics().getFilesScanned(),
                                            job.getStatistics().getFilesBackedUp(),
                                            job.getStatistics().getIncrementalSkipped(),
                                            job.getStatistics().getDuplicatesSkipped(),
                                            job.getStatistics().getFailedFiles(),
                                            job.getStatistics().getCompressedBytes(),
                                            spaceSaved
                                    )
                                    .message("Backup completed successfully")
                                    .build()
                    );
                }
            } catch (Exception e) {
                job.setStatus(BackupStatus.FAILED);
                String err = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                job.setErrorMessage(err);
                logger.error("Backup job [{}] failed: {}", job.getId(), err, e);

                if (eventPublisher != null) {
                    eventPublisher.publish(
                            BackupEvent.builder(job.getId(), BackupEventType.BACKUP_FAILED)
                                    .message("Backup failed: " + err)
                                    .build()
                    );
                }
            } finally {
                job.setCompletedAt(LocalDateTime.now());
            }
        });

        return BackupJobResponse.from(job);
    }

    @Override
    public BackupStatistics executeBackup(String folderPath, int workerCount) {
        if (folderPath == null || folderPath.isBlank()) {
            throw new IllegalArgumentException("Source folder path cannot be blank");
        }
        Path path = Path.of(folderPath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Source directory does not exist: " + folderPath);
        }
        int workers = workerCount > 0 ? workerCount : defaultWorkers;
        return backupManager.startBackup(folderPath, workers);
    }

    @Override
    public BackupJobResponse getBackupJob(String id) {
        BackupJob job = jobRegistry.findById(id)
                .orElseThrow(() -> new BackupNotFoundException("Backup not found with ID: " + id));
        return BackupJobResponse.from(job);
    }

    @Override
    public List<BackupJobResponse> getAllBackupJobs() {
        return jobRegistry.findAll()
                .stream()
                .map(BackupJobResponse::from)
                .toList();
    }

    @Override
    public RestoreResponse restoreBackup(String backupId, RestoreRequest request) {
        if (backupId != null && !backupId.isBlank()) {
            jobRegistry.findById(backupId)
                    .orElseThrow(() -> new BackupNotFoundException("Backup not found with ID: " + backupId));
        }

        try {
            int restoredCount;
            if (request != null && request.getTargetDirectory() != null && !request.getTargetDirectory().isBlank()) {
                RestoreManager customRestoreManager = new RestoreManager(Path.of(request.getTargetDirectory()));
                restoredCount = customRestoreManager.restoreAll();
            } else {
                restoredCount = restoreManager.restoreAll();
            }
            return new RestoreResponse(
                    "SUCCESS",
                    "Files restored successfully from backup metadata catalog",
                    restoredCount
            );
        } catch (Exception e) {
            logger.error("Restore operation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Restore operation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int restoreAll() {
        return restoreManager.restoreAll();
    }

    private void validateBackupRequest(BackupRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Backup request cannot be null");
        }
        if (request.getSource() == null || request.getSource().isBlank()) {
            throw new IllegalArgumentException("Source directory path cannot be blank");
        }
        Path path = Path.of(request.getSource());
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Source directory does not exist: " + request.getSource());
        }
        if (request.getWorkers() != null && request.getWorkers() <= 0) {
            throw new IllegalArgumentException("Worker count must be greater than zero");
        }
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Gracefully shutting down BackupService job executor...");
        jobExecutor.shutdown();
        try {
            if (!jobExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                jobExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            jobExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
