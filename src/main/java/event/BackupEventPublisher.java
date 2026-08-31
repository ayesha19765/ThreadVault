package event;

import java.util.function.Consumer;

/**
 * Interface defining domain event publishing and subscription for backup operations.
 *
 * <p>Completely decoupled from HTTP/SSE or web protocols.</p>
 */
public interface BackupEventPublisher {

    /**
     * Publishes a backup progress or lifecycle event.
     *
     * @param event domain event to publish
     */
    void publish(BackupEvent event);

    /**
     * Registers a listener for events of a specific backup job.
     *
     * @param backupId backup job identifier
     * @param listener consumer to receive events
     */
    void subscribe(String backupId, Consumer<BackupEvent> listener);

    /**
     * Unregisters a listener for events of a specific backup job.
     *
     * @param backupId backup job identifier
     * @param listener consumer to remove
     */
    void unsubscribe(String backupId, Consumer<BackupEvent> listener);
}

