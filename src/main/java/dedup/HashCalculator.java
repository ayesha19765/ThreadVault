package dedup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public class HashCalculator {

    public String calculateSHA256(Path file) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            try (InputStream input =
                         Files.newInputStream(file)) {

                byte[] buffer = new byte[8192];

                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {

                    digest.update(buffer, 0, bytesRead);

                }

            }

            byte[] hash = digest.digest();

            StringBuilder builder = new StringBuilder();

            for (byte b : hash) {

                builder.append(
                        String.format("%02x", b));

            }

            return builder.toString();

        } catch (Exception e) {

            throw new RuntimeException(e);

        }

    }

}