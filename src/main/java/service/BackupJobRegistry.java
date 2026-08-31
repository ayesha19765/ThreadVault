package service;

import model.BackupJob;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory thread-safe registry holding active and completed backup jobs.
 */
@Component
public class BackupJobRegistry {

    private final ConcurrentMap<String, BackupJob> jobs = new ConcurrentHashMap<>();

    /**
     * Registers a new backup job.
     *
     * @param job backup job to register
     */
    public void register(BackupJob job) {
        jobs.put(job.getId(), job);
    }

    /**
     * Finds a backup job by its unique identifier.
     *
     * @param id backup job identifier
     * @return Optional containing the job if found
     */
    public Optional<BackupJob> findById(String id) {
        return Optional.ofNullable(jobs.get(id));
    }

    /**
     * Retrieves all registered backup jobs, sorted by creation timestamp descending.
     *
     * @return list of backup jobs
     */
    public List<BackupJob> findAll() {
        List<BackupJob> list = new ArrayList<>(jobs.values());
        list.sort(Comparator.comparing(BackupJob::getCreatedAt).reversed());
        return list;
    }

    /**
     * Returns total number of registered jobs.
     *
     * @return job count
     */
    public int count() {
        return jobs.size();
    }
}
