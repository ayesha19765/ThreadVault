package event;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Thread-safe in-memory event distribution hub for ThreadVault backup operations.
 *
 * <p>Uses concurrent data structures to enable high-throughput event dispatch
 * without blocking worker threads or leaking subscriber references upon completion.</p>
 */
@Component
public class BackupEventHub implements BackupEventPublisher {

    private final ConcurrentMap<String, List<Consumer<BackupEvent>>> subscriberMap =
            new ConcurrentHashMap<>();

    @Override
    public void publish(BackupEvent event) {
        if (event == null || event.getBackupId() == null) {
            return;
        }

        String backupId = event.getBackupId();
        List<Consumer<BackupEvent>> listeners = subscriberMap.get(backupId);

        if (listeners != null && !listeners.isEmpty()) {
            for (Consumer<BackupEvent> listener : listeners) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    // Isolate subscriber errors to protect worker and other listeners
                    System.err.printf("[EventHub] Error delivering event to subscriber for backup %s: %s%n",
                            backupId, e.getMessage());
                }
            }
        }

        // Automatically clean up subscribers when a terminal event is published
        if (event.getType() == BackupEventType.BACKUP_COMPLETED || event.getType() == BackupEventType.BACKUP_FAILED) {
            subscriberMap.remove(backupId);
        }
    }

    @Override
    public void subscribe(String backupId, Consumer<BackupEvent> listener) {
        if (backupId == null || listener == null) {
            return;
        }
        subscriberMap.computeIfAbsent(backupId, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    public void unsubscribe(String backupId, Consumer<BackupEvent> listener) {
        if (backupId == null || listener == null) {
            return;
        }
        List<Consumer<BackupEvent>> listeners = subscriberMap.get(backupId);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                subscriberMap.remove(backupId, listeners);
            }
        }
    }

    /**
     * Returns count of active subscribers for a given backup ID.
     */
    public int getSubscriberCount(String backupId) {
        List<Consumer<BackupEvent>> listeners = subscriberMap.get(backupId);
        return listeners != null ? listeners.size() : 0;
    }
}

