package compression;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CompressionManager {

    private static final Path BACKUP_DIRECTORY =
            Path.of("backup_storage");

    public CompressionManager() {

        try {

            Files.createDirectories(BACKUP_DIRECTORY);

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }

    public Path compress(Path sourceFile,
                         String hash) {

        Path zipFile =
                BACKUP_DIRECTORY.resolve(hash + ".zip");

        if (Files.exists(zipFile))
            return zipFile;

        try (
                FileInputStream fis =
                        new FileInputStream(sourceFile.toFile());

                FileOutputStream fos =
                        new FileOutputStream(zipFile.toFile());

                ZipOutputStream zos =
                        new ZipOutputStream(fos)
        ) {

            ZipEntry entry =
                    new ZipEntry(
                            sourceFile.getFileName().toString());

            zos.putNextEntry(entry);

            byte[] buffer = new byte[8192];

            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {

                zos.write(buffer, 0, bytesRead);

            }

            zos.closeEntry();

            return zipFile;

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }

}