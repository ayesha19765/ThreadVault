package benchmark;

import backup.BackupManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import stats.BackupStatistics;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Reproducible benchmarking runner for ThreadVault.
 *
 * <p>Measures throughput, speedup, deduplication efficiency, and storage savings
 * across Small (1,000 files), Medium (10,000 files), and Duplicate-Heavy (10,000 files)
 * workloads comparing Sequential (1 worker) vs Concurrent (4 and 8 workers).</p>
 */
public class BackupBenchmarkRunner {

    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(String[] args) throws Exception {
        System.out.println("================================================================================");
        System.out.println("                THREADVAULT PRODUCTION BENCHMARK SUITE                          ");
        System.out.println("================================================================================");

        Map<String, Object> systemEnv = getSystemEnvironment();
        System.out.println("Environment: " + systemEnv.get("os") + " | " + systemEnv.get("cpuCores") + " Cores | Java " + systemEnv.get("javaVersion"));
        System.out.println();

        Path benchmarkRoot = Files.createTempDirectory("threadvault_benchmarks_");

        try {
            List<Map<String, Object>> workloadResults = new ArrayList<>();

            // Benchmark A: Small Dataset (1,000 files)
            System.out.println(">>> Running Benchmark A: Small Dataset (1,000 files)...");
            Path smallDataset = benchmarkRoot.resolve("dataset_small");
            generateDataset(smallDataset, 1000, 1000, 1024, 15360); // 1K - 15KB per file
            Map<String, Object> resultA = runBenchmarkWorkload("Small Dataset", smallDataset, 3);
            workloadResults.add(resultA);
            System.out.println("Finished Benchmark A.\n");

            // Benchmark B: Medium Dataset (10,000 files)
            System.out.println(">>> Running Benchmark B: Medium Dataset (10,000 files)...");
            Path mediumDataset = benchmarkRoot.resolve("dataset_medium");
            generateDataset(mediumDataset, 10000, 10000, 1024, 10240); // 1K - 10KB per file
            Map<String, Object> resultB = runBenchmarkWorkload("Medium Dataset", mediumDataset, 3);
            workloadResults.add(resultB);
            System.out.println("Finished Benchmark B.\n");

            // Benchmark C: Duplicate-Heavy Dataset (10,000 files, 70% duplicates)
            System.out.println(">>> Running Benchmark C: Duplicate-Heavy Dataset (10,000 files, ~70% duplicate content)...");
            Path dupDataset = benchmarkRoot.resolve("dataset_dup_heavy");
            generateDataset(dupDataset, 10000, 3000, 2048, 12288); // 3,000 unique pools distributed over 10,000 files
            Map<String, Object> resultC = runBenchmarkWorkload("Duplicate-Heavy Dataset", dupDataset, 3);
            workloadResults.add(resultC);
            System.out.println("Finished Benchmark C.\n");

            // Build benchmark summary
            Map<String, Object> finalOutput = new LinkedHashMap<>();
            finalOutput.put("timestamp", new Date().toString());
            finalOutput.put("system", systemEnv);
            finalOutput.put("benchmarks", workloadResults);

            // Write JSON result to project benchmarks/results/latest.json
            Path resultsDir = Path.of("benchmarks", "results");
            Files.createDirectories(resultsDir);
            Path jsonFile = resultsDir.resolve("latest.json");
            mapper.writeValue(jsonFile.toFile(), finalOutput);
            System.out.println("Benchmark results saved to: " + jsonFile.toAbsolutePath());

            // Print summary report
            printSummaryReport(finalOutput);

        } finally {
            deleteRecursively(benchmarkRoot.toFile());
        }
    }

    private static Map<String, Object> runBenchmarkWorkload(String name, Path sourceDir, int iterations) throws Exception {
        long totalInputBytes = calculateDirectorySize(sourceDir);
        int totalFiles = countFiles(sourceDir);

        // Run Sequential (1 worker)
        List<Long> seqTimes = new ArrayList<>();
        BackupStatistics seqStats = null;
        for (int i = 0; i < iterations; i++) {
            Path runStorage = Files.createTempDirectory("bench_storage_seq_");
            Path runMeta = Files.createTempDirectory("bench_meta_seq_");
            try {
                System.setProperty("THREADVAULT_STORAGE_PATH", runStorage.toString());
                System.setProperty("THREADVAULT_METADATA_PATH", runMeta.toString());

                BackupManager manager = new BackupManager();
                BackupStatistics stats = new BackupStatistics();
                long start = System.nanoTime();
                manager.startBackup(sourceDir.toString(), 1, stats);
                long elapsedMs = (System.nanoTime() - start) / 1_000_000;
                seqTimes.add(elapsedMs);
                seqStats = stats;
            } finally {
                deleteRecursively(runStorage.toFile());
                deleteRecursively(runMeta.toFile());
            }
        }

        // Run Concurrent (4 workers)
        List<Long> conc4Times = new ArrayList<>();
        BackupStatistics conc4Stats = null;
        long storedBytes = 0;
        for (int i = 0; i < iterations; i++) {
            Path runStorage = Files.createTempDirectory("bench_storage_conc4_");
            Path runMeta = Files.createTempDirectory("bench_meta_conc4_");
            try {
                System.setProperty("THREADVAULT_STORAGE_PATH", runStorage.toString());
                System.setProperty("THREADVAULT_METADATA_PATH", runMeta.toString());

                BackupManager manager = new BackupManager();
                BackupStatistics stats = new BackupStatistics();
                long start = System.nanoTime();
                manager.startBackup(sourceDir.toString(), 4, stats);
                long elapsedMs = (System.nanoTime() - start) / 1_000_000;
                conc4Times.add(elapsedMs);
                conc4Stats = stats;
                if (i == 0) {
                    storedBytes = calculateDirectorySize(runStorage);
                }
            } finally {
                deleteRecursively(runStorage.toFile());
                deleteRecursively(runMeta.toFile());
            }
        }

        // Run Concurrent (8 workers)
        List<Long> conc8Times = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            Path runStorage = Files.createTempDirectory("bench_storage_conc8_");
            Path runMeta = Files.createTempDirectory("bench_meta_conc8_");
            try {
                System.setProperty("THREADVAULT_STORAGE_PATH", runStorage.toString());
                System.setProperty("THREADVAULT_METADATA_PATH", runMeta.toString());

                BackupManager manager = new BackupManager();
                BackupStatistics stats = new BackupStatistics();
                long start = System.nanoTime();
                manager.startBackup(sourceDir.toString(), 8, stats);
                long elapsedMs = (System.nanoTime() - start) / 1_000_000;
                conc8Times.add(elapsedMs);
            } finally {
                deleteRecursively(runStorage.toFile());
                deleteRecursively(runMeta.toFile());
            }
        }

        long avgSeqMs = (long) seqTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long avgConc4Ms = (long) conc4Times.stream().mapToLong(Long::longValue).average().orElse(0);
        long avgConc8Ms = (long) conc8Times.stream().mapToLong(Long::longValue).average().orElse(0);

        double throughputMBs = ((double) totalInputBytes / (1024 * 1024)) / ((double) avgConc4Ms / 1000.0);
        double dedupRatio = totalFiles > 0 ? ((double) conc4Stats.getDuplicatesSkipped() / totalFiles) * 100.0 : 0.0;
        double storageSaved = totalInputBytes > 0 ? (1.0 - ((double) storedBytes / totalInputBytes)) * 100.0 : 0.0;
        double speedup4x = avgConc4Ms > 0 ? (double) avgSeqMs / avgConc4Ms : 1.0;
        double speedup8x = avgConc8Ms > 0 ? (double) avgSeqMs / avgConc8Ms : 1.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workloadName", name);
        result.put("totalFiles", totalFiles);
        result.put("totalInputBytes", totalInputBytes);
        result.put("totalInputMB", Math.round(((double) totalInputBytes / (1024 * 1024)) * 100.0) / 100.0);
        result.put("storedBytes", storedBytes);
        result.put("storedMB", Math.round(((double) storedBytes / (1024 * 1024)) * 100.0) / 100.0);
        result.put("filesDeduplicated", conc4Stats.getDuplicatesSkipped());
        result.put("filesBackedUp", conc4Stats.getFilesBackedUp());
        result.put("sequentialTimeMs", avgSeqMs);
        result.put("concurrent4TimeMs", avgConc4Ms);
        result.put("concurrent8TimeMs", avgConc8Ms);
        result.put("throughputMBs", Math.round(throughputMBs * 10.0) / 10.0);
        result.put("deduplicationPercentage", Math.round(dedupRatio * 10.0) / 10.0);
        result.put("storageSavedPercentage", Math.round(storageSaved * 10.0) / 10.0);
        result.put("speedup4x", Math.round(speedup4x * 100.0) / 100.0);
        result.put("speedup8x", Math.round(speedup8x * 100.0) / 100.0);

        return result;
    }

    private static void generateDataset(Path targetDir, int totalFiles, int uniquePoolSize, int minSizeBytes, int maxSizeBytes) throws IOException {
        Files.createDirectories(targetDir);
        Random random = new Random(42); // deterministic seed

        // Generate unique binary/text templates
        byte[][] templates = new byte[uniquePoolSize][];
        for (int i = 0; i < uniquePoolSize; i++) {
            int size = minSizeBytes + random.nextInt(Math.max(1, maxSizeBytes - minSizeBytes));
            byte[] data = new byte[size];
            random.nextBytes(data);
            templates[i] = data;
        }

        // Distribute across nested directory tree
        for (int i = 0; i < totalFiles; i++) {
            int folderId = i % 20;
            Path folder = targetDir.resolve("folder_" + folderId).resolve("sub_" + (i % 5));
            Files.createDirectories(folder);

            byte[] content = templates[i % uniquePoolSize];
            Path filePath = folder.resolve("file_" + i + ".dat");
            Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private static long calculateDirectorySize(Path path) throws IOException {
        try (var stream = Files.walk(path)) {
            return stream.filter(Files::isRegularFile).mapToLong(p -> {
                try {
                    return Files.size(p);
                } catch (IOException e) {
                    return 0L;
                }
            }).sum();
        }
    }

    private static int countFiles(Path path) throws IOException {
        try (var stream = Files.walk(path)) {
            return (int) stream.filter(Files::isRegularFile).count();
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    private static Map<String, Object> getSystemEnvironment() {
        Map<String, Object> env = new LinkedHashMap<>();
        env.put("os", System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")");
        env.put("javaVersion", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        env.put("cpuCores", Runtime.getRuntime().availableProcessors());
        env.put("maxHeapMB", Runtime.getRuntime().maxMemory() / (1024 * 1024));
        return env;
    }

    private static void printSummaryReport(Map<String, Object> finalOutput) {
        System.out.println("================================================================================");
        System.out.println("                           BENCHMARK SUMMARY REPORT                             ");
        System.out.println("================================================================================");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) finalOutput.get("benchmarks");

        System.out.printf("%-24s | %-7s | %-9s | %-10s | %-10s | %-12s | %-13s%n",
                "Workload", "Files", "Input MB", "Seq (1w)", "Conc (4w)", "Throughput", "Storage Saved");
        System.out.println("--------------------------------------------------------------------------------");

        for (Map<String, Object> row : list) {
            System.out.printf("%-24s | %-7d | %-7.2f MB | %-8d ms | %-8d ms | %-7.1f MB/s | %-5.1f%%%n",
                    row.get("workloadName"),
                    row.get("totalFiles"),
                    row.get("totalInputMB"),
                    row.get("sequentialTimeMs"),
                    row.get("concurrent4TimeMs"),
                    row.get("throughputMBs"),
                    row.get("storageSavedPercentage")
            );
        }
        System.out.println("================================================================================\n");
    }
}
