package event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class BackupEventPublisherTest {

    private BackupEventHub eventHub;

    @BeforeEach
    void setUp() {
        eventHub = new BackupEventHub();
    }

    @Test
    void testPublishEvent_DeliversToSubscribers() {
        String backupId = "test-job-1";
        List<BackupEvent> received = new ArrayList<>();

        eventHub.subscribe(backupId, received::add);

        BackupEvent event1 = BackupEvent.builder(backupId, BackupEventType.BACKUP_STARTED)
                .message("Starting backup")
                .build();

        BackupEvent event2 = BackupEvent.builder(backupId, BackupEventType.FILE_PROCESSED)
                .file("test.txt")
                .fileSize(1024L)
                .stats(1, 1, 0, 0, 0, 512, 50.0)
                .build();

        eventHub.publish(event1);
        eventHub.publish(event2);

        assertEquals(2, received.size());
        assertEquals(BackupEventType.BACKUP_STARTED, received.get(0).getType());
        assertEquals(BackupEventType.FILE_PROCESSED, received.get(1).getType());
        assertEquals("test.txt", received.get(1).getFile());
        assertEquals(1024L, received.get(1).getFileSize());
        assertEquals(50.0, received.get(1).getSpaceSavedPercentage());
    }

    @Test
    void testPublishCompletedEvent_CleansUpSubscribers() {
        String backupId = "test-job-2";
        AtomicInteger eventCount = new AtomicInteger(0);

        eventHub.subscribe(backupId, e -> eventCount.incrementAndGet());
        assertEquals(1, eventHub.getSubscriberCount(backupId));

        BackupEvent completedEvent = BackupEvent.builder(backupId, BackupEventType.BACKUP_COMPLETED)
                .message("All done")
                .build();

        eventHub.publish(completedEvent);

        assertEquals(1, eventCount.get());
        // Verify subscriber collection was cleaned up after terminal event
        assertEquals(0, eventHub.getSubscriberCount(backupId));
    }

    @Test
    void testUnsubscribe_RemovesListener() {
        String backupId = "test-job-3";
        List<BackupEvent> received = new ArrayList<>();
        java.util.function.Consumer<BackupEvent> listener = received::add;

        eventHub.subscribe(backupId, listener);
        assertEquals(1, eventHub.getSubscriberCount(backupId));

        eventHub.publish(BackupEvent.builder(backupId, BackupEventType.BACKUP_STARTED).build());
        assertEquals(1, received.size());

        eventHub.unsubscribe(backupId, listener);
        assertEquals(0, eventHub.getSubscriberCount(backupId));

        eventHub.publish(BackupEvent.builder(backupId, BackupEventType.FILE_PROCESSED).file("f1.txt").build());
        // Should not receive subsequent events
        assertEquals(1, received.size());
    }

    @Test
    void testListenerException_DoesNotCrashPublisher() {
        String backupId = "test-job-4";
        List<BackupEvent> secondListenerEvents = new ArrayList<>();

        // Failing listener
        eventHub.subscribe(backupId, e -> {
            throw new RuntimeException("Simulated subscriber failure");
        });

        // Healthy listener
        eventHub.subscribe(backupId, secondListenerEvents::add);

        assertDoesNotThrow(() -> {
            eventHub.publish(BackupEvent.builder(backupId, BackupEventType.FILE_PROCESSED).file("safe.txt").build());
        });

        assertEquals(1, secondListenerEvents.size());
    }

    @Test
    void testConcurrentPublishing_ThreadSafe() throws InterruptedException {
        String backupId = "test-job-concurrent";
        List<BackupEvent> received = Collections.synchronizedList(new ArrayList<>());
        eventHub.subscribe(backupId, received::add);

        int threadCount = 10;
        int eventsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < eventsPerThread; i++) {
                        eventHub.publish(
                                BackupEvent.builder(backupId, BackupEventType.FILE_PROCESSED)
                                        .file("thread-" + threadIndex + "-file-" + i + ".txt")
                                        .fileSize(100L)
                                        .build()
                        );
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(threadCount * eventsPerThread, received.size());
    }
}

