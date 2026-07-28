package dedup;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public class DeduplicationEngine {

    private final ConcurrentHashMap<String, Path> hashIndex =
            new ConcurrentHashMap<>();

    public boolean isDuplicate(String hash,
                               Path file) {

        return hashIndex.putIfAbsent(hash, file) != null;

    }

}