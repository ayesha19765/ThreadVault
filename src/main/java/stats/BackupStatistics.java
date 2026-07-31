package stats;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Thread-safe statistics collector for backup operations.
 *
 * Supports concurrent updates from multiple backup workers
 * using atomic counters and LongAdder for high-frequency
 * byte aggregation.
 */
public class BackupStatistics {

    private final AtomicInteger filesScanned =
            new AtomicInteger();

    private final AtomicInteger filesBackedUp =
            new AtomicInteger();

    private final AtomicInteger duplicatesSkipped =
            new AtomicInteger();

    private final AtomicInteger incrementalSkipped =
            new AtomicInteger();

    private final AtomicInteger failedFiles =
            new AtomicInteger();

    private final LongAdder originalBytes =
            new LongAdder();

    private final LongAdder compressedBytes =
            new LongAdder();


    public void fileScanned() {

        filesScanned.incrementAndGet();

    }


    public void fileBackedUp(
            long originalSize,
            long compressedSize
    ) {

        filesBackedUp.incrementAndGet();

        originalBytes.add(originalSize);

        compressedBytes.add(compressedSize);

    }


    public void duplicateSkipped() {

        duplicatesSkipped.incrementAndGet();

    }


    public void incrementalSkipped() {

        incrementalSkipped.incrementAndGet();

    }


    public void fileFailed() {

        failedFiles.incrementAndGet();

    }


    public int getFilesScanned() {

        return filesScanned.get();

    }


    public int getFilesBackedUp() {

        return filesBackedUp.get();

    }


    public int getDuplicatesSkipped() {

        return duplicatesSkipped.get();

    }


    public int getIncrementalSkipped() {

        return incrementalSkipped.get();

    }


    public int getFailedFiles() {

        return failedFiles.get();

    }


    public long getOriginalBytes() {

        return originalBytes.sum();

    }


    public long getCompressedBytes() {

        return compressedBytes.sum();

    }


    public int getTotalSkipped() {

        return getDuplicatesSkipped()
                +
                getIncrementalSkipped();

    }


    public void printSummary() {

        System.out.println();

        System.out.println(
                "========== Backup Statistics =========="
        );


        System.out.printf(
                "Files Scanned         : %d%n",
                getFilesScanned()
        );


        System.out.printf(
                "Files Backed Up       : %d%n",
                getFilesBackedUp()
        );


        System.out.printf(
                "Duplicates Skipped    : %d%n",
                getDuplicatesSkipped()
        );


        System.out.printf(
                "Incremental Skipped   : %d%n",
                getIncrementalSkipped()
        );


        System.out.printf(
                "Total Skipped         : %d%n",
                getTotalSkipped()
        );


        System.out.printf(
                "Failed Files          : %d%n",
                getFailedFiles()
        );


        System.out.printf(
                "Original Size (Bytes) : %d%n",
                getOriginalBytes()
        );


        System.out.printf(
                "Compressed Size(Bytes): %d%n",
                getCompressedBytes()
        );


        if (getOriginalBytes() > 0) {

            double compressionPercentage =
                    (getCompressedBytes() * 100.0)
                            /
                            getOriginalBytes();


            double spaceSaved =
                    100.0 - compressionPercentage;


            System.out.printf(
                    "Compression Percentage: %.2f%%%n",
                    compressionPercentage
            );


            System.out.printf(
                    "Space Saved           : %.2f%%%n",
                    spaceSaved
            );

        }


        System.out.println(
                "======================================="
        );

    }

}