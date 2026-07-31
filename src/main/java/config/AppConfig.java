package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads and provides access to application configuration.
 *
 * <p>Configuration values are loaded from the
 * {@code config.properties} file located in the application's
 * resources directory.</p>
 *
 * <p>This class acts as a single configuration provider for
 * components such as backup workers, scheduler, and watcher.</p>
 */
public class AppConfig {

    private final Properties properties =
            new Properties();

    /**
     * Creates an AppConfig instance and loads configuration
     * from {@code config.properties}.
     *
     * @throws RuntimeException if the configuration file
     *                          is missing or cannot be loaded
     */
    public AppConfig() {

        try (InputStream input =
                     getClass()
                             .getClassLoader()
                             .getResourceAsStream(
                                     "config.properties")) {

            if (input == null) {

                throw new RuntimeException(
                        "Missing config.properties file");

            }

            properties.load(input);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to load configuration",
                    e);

        }

    }

    /**
     * Returns the number of worker threads used by the backup engine.
     *
     * <p>If the value is not configured, the default value is 4.</p>
     *
     * @return number of backup worker threads
     * @throws IllegalArgumentException if worker count is zero
     *                                  or negative
     */
    public int getWorkerCount() {

        int workers =
                Integer.parseInt(
                        properties.getProperty(
                                "workers",
                                "4"));

        if (workers <= 0) {

            throw new IllegalArgumentException(
                    "Worker count must be greater than zero");

        }

        return workers;

    }

    /**
     * Returns the directory that should be backed up.
     *
     * @return configured backup directory path
     * @throws IllegalArgumentException if the directory
     *                                  is not configured
     */
    public String getBackupDirectory() {

        String directory =
                properties.getProperty(
                        "backup.directory");

        if (directory == null ||
                directory.isBlank()) {

            throw new IllegalArgumentException(
                    "backup.directory is not configured");

        }

        return directory;

    }

    /**
     * Checks whether real-time watch mode is enabled.
     *
     * <p>If the property is missing, watch mode is disabled
     * by default.</p>
     *
     * @return true if directory watching is enabled,
     *         otherwise false
     */
    public boolean isWatchMode() {

        return Boolean.parseBoolean(
                properties.getProperty(
                        "watch.mode",
                        "false"));

    }

}