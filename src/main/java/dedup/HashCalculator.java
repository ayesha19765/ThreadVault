package dedup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

/**
 * Calculates SHA-256 hash of a file.
 *
 * Uses streaming reads so large files can be
 * processed without loading the entire file
 * into memory.
 */
public class HashCalculator {

    private static final int BUFFER_SIZE = 8192;

    public String calculateSHA256(Path file) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            try (InputStream input =
                         Files.newInputStream(file)) {

                byte[] buffer =
                        new byte[BUFFER_SIZE];

                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {

                    digest.update(
                            buffer,
                            0,
                            bytesRead
                    );

                }

            }

            byte[] hash =
                    digest.digest();

            StringBuilder builder =
                    new StringBuilder();

            for (byte b : hash) {

                builder.append(
                        String.format(
                                "%02x",
                                b & 0xff
                        )
                );

            }

            return builder.toString();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to read file for SHA-256 calculation: "
                            + file,
                    e
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to calculate SHA-256 hash for file: "
                            + file,
                    e
            );

        }

    }

}