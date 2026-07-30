package restore;

import metadata.FileMetadata;
import metadata.MetadataStore;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class RestoreManager {

    private static final Path RESTORE_DIRECTORY =
            Path.of("restore");

    private final MetadataStore metadataStore;

    public RestoreManager() {

        metadataStore = new MetadataStore();

        try {

            Files.createDirectories(RESTORE_DIRECTORY);

        }

        catch(IOException e){

            throw new RuntimeException(e);

        }

    }

    public void restoreAll() {

        List<FileMetadata> metadataList =
                metadataStore.getAllMetadata();

        for(FileMetadata metadata : metadataList){

            restore(metadata);

        }

    }

    private void restore(FileMetadata metadata){

        Path zipFile =
                Path.of(metadata.getBackupPath());

        Path outputFile =
                RESTORE_DIRECTORY.resolve(
                        metadata.getOriginalPath());

        try{

            Files.createDirectories(
                    outputFile.getParent());

            try(

                    ZipInputStream zis =
                            new ZipInputStream(
                                    new FileInputStream(
                                            zipFile.toFile()));

            ){

                ZipEntry entry =
                        zis.getNextEntry();

                if(entry==null)
                    return;

                try(

                        FileOutputStream fos =
                                new FileOutputStream(
                                        outputFile.toFile());

                ){

                    byte[] buffer =
                            new byte[8192];

                    int bytesRead;

                    while((bytesRead=zis.read(buffer))!=-1){

                        fos.write(buffer,0,bytesRead);

                    }

                }

            }

            System.out.println(
                    "Restored : "
                            + outputFile);

        }

        catch(IOException e){

            throw new RuntimeException(e);

        }

    }

}