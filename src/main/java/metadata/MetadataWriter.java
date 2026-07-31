package metadata;

import java.util.concurrent.BlockingQueue;

/**
 * Dedicated consumer responsible for writing
 * backup metadata to disk.
 *
 * A single writer thread prevents multiple
 * backup workers from writing to the metadata
 * file concurrently.
 */
public class MetadataWriter implements Runnable {

    /*
     * Sentinel object indicating that no more
     * metadata will be produced.
     */
    public static final FileMetadata POISON =
            new FileMetadata();

    private final BlockingQueue<FileMetadata> metadataQueue;
    private final MetadataStore metadataStore;

    public MetadataWriter(
            BlockingQueue<FileMetadata> metadataQueue,
            MetadataStore metadataStore
    ) {

        this.metadataQueue = metadataQueue;
        this.metadataStore = metadataStore;

    }

    @Override
    public void run() {

        try {

            while (true) {

                final FileMetadata metadata =
                        metadataQueue.take();

                if (metadata == POISON) {

                    break;

                }

                metadataStore.saveMetadata(metadata);

            }

            System.out.printf(
                    "[%s] Shutting down.%n",
                    Thread.currentThread().getName()
            );

        } catch (InterruptedException e) {

            System.err.println(
                    "Metadata Writer interrupted."
            );

            Thread.currentThread().interrupt();

        }

    }

}