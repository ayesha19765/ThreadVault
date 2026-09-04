package compression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Handles compression of files into ZIP format.
 *
 * Compressed files are stored using their hash as
 * the filename, enabling content-addressable storage
 * and deduplication.
 *
 * Writes are performed atomically using temporary files
 * to prevent concurrent read of half-written archives.
 */
public class CompressionManager {

    private static final Logger logger = LoggerFactory.getLogger(CompressionManager.class);
    private static final Path DEFAULT_BACKUP_FOLDER =
            Path.of(System.getenv().getOrDefault("THREADVAULT_STORAGE_PATH", "backup_storage"));
    private static Path getDefaultBackupFolder() {
        return Path.of(System.getProperty("THREADVAULT_STORAGE_PATH",
                System.getenv().getOrDefault("THREADVAULT_STORAGE_PATH", "backup_storage")));
    }

    private static final int BUFFER_SIZE = 8192;

    private final Path backupFolder;

    public CompressionManager() {
        this(getDefaultBackupFolder());
    }

    public CompressionManager(Path backupFolder) {
        this.backupFolder = backupFolder != null ? backupFolder : getDefaultBackupFolder();
        try {
            Files.createDirectories(this.backupFolder);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to create backup directory: "
                            + this.backupFolder,
                    e
            );
        }
    }

    /**
     * Compresses a source file into ZIP format atomically.
     *
     * @param sourceFile file to compress
     * @param hash content hash used as backup identity
     * @return path of the compressed backup file
     */
    public Path compress(
            Path sourceFile,
            String hash
    ) {

        if (!Files.exists(sourceFile)) {
            throw new RuntimeException(
                    "Source file does not exist: "
                            + sourceFile
            );
        }

        Path zipFile =
                backupFolder.resolve(hash + ".zip");

        if (Files.exists(zipFile)) {
            return zipFile;
        }

        Path tempFile =
                backupFolder.resolve(hash + ".zip.tmp." + UUID.randomUUID());

        try {
            try (
                    InputStream input =
                            Files.newInputStream(sourceFile);

                    OutputStream output =
                            Files.newOutputStream(tempFile);

                    ZipOutputStream zos =
                            new ZipOutputStream(output)
            ) {
                ZipEntry entry =
                        new ZipEntry(
                                sourceFile.getFileName()
                                        .toString()
                        );

                zos.putNextEntry(entry);

                byte[] buffer =
                        new byte[BUFFER_SIZE];

                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    zos.write(
                            buffer,
                            0,
                            bytesRead
                    );
                }

                zos.closeEntry();
            }

            try {
                Files.move(tempFile, zipFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                Files.move(tempFile, zipFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return zipFile;

        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
            }
            throw new RuntimeException(
                    "Failed to compress file: "
                            + sourceFile,
                    e
            );
        }
    }
}
