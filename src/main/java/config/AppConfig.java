package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private final Properties properties =
            new Properties();

    public AppConfig() {

        try (InputStream input =
                     getClass()
                             .getClassLoader()
                             .getResourceAsStream(
                                     "config.properties")) {

            if (input != null) {

                properties.load(input);

            }

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }

    public int getWorkerCount() {

        return Integer.parseInt(
                properties.getProperty(
                        "workers",
                        "4"));

    }

    public String getBackupDirectory() {

        return properties.getProperty(
                "backup.directory");

    }

    public boolean isWatchMode() {

        return Boolean.parseBoolean(
                properties.getProperty(
                        "watch.mode"));

    }

}