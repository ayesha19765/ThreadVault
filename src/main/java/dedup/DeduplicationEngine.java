package dedup;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Performs content-based deduplication using file hashes.
 *
 * Each unique hash represents one stored backup object.
 *
 * Uses ConcurrentHashMap because multiple backup workers
 * may access this component concurrently.
 */
public class DeduplicationEngine {

    private final ConcurrentHashMap<String, Path> hashToFileMap =
            new ConcurrentHashMap<>();

    public boolean isDuplicate(
            String hash,
            Path file
    ) {

        /*
         * putIfAbsent provides atomic check-and-insert,
         * preventing duplicate storage when multiple
         * backup workers process the same content.
         */
        return hashToFileMap.putIfAbsent(hash, file) != null;

    }

}