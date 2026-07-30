package stats;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public class BackupStatistics {

    private final AtomicInteger filesScanned =
            new AtomicInteger();

    private final AtomicInteger filesBackedUp =
            new AtomicInteger();

    private final AtomicInteger duplicatesSkipped =
            new AtomicInteger();

    private final AtomicInteger incrementalSkipped =
            new AtomicInteger();

    private final LongAdder originalBytes =
            new LongAdder();

    private final LongAdder compressedBytes =
            new LongAdder();

    private final AtomicInteger failedFiles =
            new AtomicInteger();

    public void fileScanned() {

        filesScanned.incrementAndGet();

    }

    public void fileFailed() {

        failedFiles.incrementAndGet();

    }

    public void fileBackedUp(
            long original,
            long compressed) {

        filesBackedUp.incrementAndGet();

        originalBytes.add(original);

        compressedBytes.add(compressed);

    }

    public void duplicateSkipped() {

        duplicatesSkipped.incrementAndGet();

    }

    public void incrementalSkipped() {

        incrementalSkipped.incrementAndGet();

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

    public long getOriginalBytes() {

        return originalBytes.sum();

    }

    public long getCompressedBytes() {

        return compressedBytes.sum();

    }

    public int getFailedFiles() {

        return failedFiles.get();

    }

}