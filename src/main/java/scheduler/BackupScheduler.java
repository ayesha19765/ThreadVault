package scheduler;

import backup.BackupManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Schedules automatic backups at fixed intervals.
 *
 * Uses a single-thread scheduler to ensure that
 * backup executions do not overlap.
 */
public class BackupScheduler {

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();


    public void start(
            BackupManager manager,
            String folder,
            int intervalSeconds
    ) {

        if (intervalSeconds <= 0) {

            throw new IllegalArgumentException(
                    "Backup interval must be greater than zero."
            );

        }


        scheduler.scheduleAtFixedRate(

                () -> {

                    try {

                        System.out.println();

                        System.out.println(
                                "[Scheduler] Automatic Backup Started"
                        );


                        manager.startBackup(folder);


                        System.out.println();

                        System.out.println(
                                "[Scheduler] Next backup in "
                                        + intervalSeconds
                                        + " seconds."
                        );


                    } catch (Exception e) {

                        System.err.println(
                                "[Scheduler] Scheduled backup failed."
                        );

                        e.printStackTrace();

                    }

                },

                0,

                intervalSeconds,

                TimeUnit.SECONDS

        );

    }


    public void stop() {

        scheduler.shutdown();

        try {

            if (!scheduler.awaitTermination(
                    5,
                    TimeUnit.SECONDS
            )) {

                scheduler.shutdownNow();

            }

        } catch (InterruptedException e) {

            scheduler.shutdownNow();

            Thread.currentThread()
                    .interrupt();

        }

    }

}