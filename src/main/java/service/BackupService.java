package service;

import dto.request.BackupRequest;
import dto.request.RestoreRequest;
import dto.response.BackupJobResponse;
import dto.response.RestoreResponse;
import stats.BackupStatistics;

import java.util.List;

/**
 * Service interface defining backup and restore operations for ThreadVault.
 *
 * <p>Serves as the single integration point used by both the REST API
 * and the CLI interface.</p>
 */
public interface BackupService {

    /**
     * Submits an asynchronous backup job and immediately returns the job details.
     *
     * @param request backup configuration request
     * @return response with initial job status (QUEUED) and assigned job ID
     */
    BackupJobResponse submitBackup(BackupRequest request);

    /**
     * Executes a backup synchronously, blocking until completion.
     *
     * @param folderPath source folder path
     * @param workerCount worker thread count (<= 0 uses default)
     * @return backup statistics
     */
    BackupStatistics executeBackup(String folderPath, int workerCount);

    /**
     * Retrieves the current status and statistics of a backup job.
     *
     * @param id backup job identifier
     * @return job status and statistics response
     */
    BackupJobResponse getBackupJob(String id);

    /**
     * Returns all recent backup jobs.
     *
     * @return list of backup job responses
     */
    List<BackupJobResponse> getAllBackupJobs();

    /**
     * Triggers restore for a backup job or full catalog.
     *
     * @param backupId backup job identifier (optional)
     * @param request restore options
     * @return restore operation response
     */
    RestoreResponse restoreBackup(String backupId, RestoreRequest request);

    /**
     * Triggers restore of all backed-up files.
     *
     * @return number of restored files
     */
    int restoreAll();
}
