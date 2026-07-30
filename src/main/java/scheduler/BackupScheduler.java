package scheduler;

import backup.BackupManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BackupScheduler {

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public void start(

            BackupManager manager,

            String folder,

            int intervalSeconds

    ) {

        scheduler.scheduleAtFixedRate(

                () -> {

                    System.out.println();

                    System.out.println(
                            "Automatic Backup Started");

                    manager.startBackup(folder);

                    System.out.println();

                    System.out.println(

                            "Next backup in "

                                    + intervalSeconds

                                    + " seconds."

                    );

                },

                0,

                intervalSeconds,

                TimeUnit.SECONDS

        );

    }

    public void stop() {

        scheduler.shutdown();

    }

}