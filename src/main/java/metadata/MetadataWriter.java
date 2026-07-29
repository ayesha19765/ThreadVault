package metadata;

import java.util.concurrent.BlockingQueue;

public class MetadataWriter implements Runnable {

    public static final FileMetadata POISON = new FileMetadata();

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

                FileMetadata metadata = metadataQueue.take();

                if (metadata == POISON) {
                    break;
                }

                metadataStore.saveMetadata(metadata);
            }

            System.out.println("Metadata Writer shutting down.");

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

        }
    }
}