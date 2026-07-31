package compression;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Handles compression of files into ZIP format.
 *
 * Compressed files are stored using their hash as
 * the filename, enabling content-addressable storage
 * and deduplication.
 */
public class CompressionManager {

    private static final Path BACKUP_FOLDER =
            Path.of("backup_storage");

    private static final int BUFFER_SIZE = 8192;

    public CompressionManager() {

        try {

            Files.createDirectories(BACKUP_FOLDER);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to create backup directory: "
                            + BACKUP_FOLDER,
                    e
            );

        }

    }

    /**
     * Compresses a source file into ZIP format.
     *
     * The generated ZIP filename is based on the
     * file hash, allowing duplicate content to share
     * the same backup object.
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
                BACKUP_FOLDER.resolve(hash + ".zip");


        /*
         * Already compressed content exists.
         * Reuse it for deduplication.
         */
        if (Files.exists(zipFile)) {

            return zipFile;

        }


        try (
                InputStream input =
                        Files.newInputStream(sourceFile);

                OutputStream output =
                        Files.newOutputStream(zipFile);

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


            return zipFile;


        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to compress file: "
                            + sourceFile,
                    e
            );

        }

    }

}