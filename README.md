# ThreadVault

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-16.3-black.svg)](https://nextjs.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://www.docker.com/)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0%20%7C%20Swagger-green.svg)](http://localhost:8080/swagger-ui.html)
[![Architecture](https://img.shields.io/badge/Architecture-Producer--Consumer-success.svg)]()

**ThreadVault** is a high-performance, concurrent incremental backup and content-based deduplication engine built with **Java 21**, **Spring Boot**, and **Next.js**. It coordinates bounded multithreaded worker pools to scan filesystem hierarchies, skip unchanged files via timestamp metadata checks, compute SHA-256 cryptographic digests, deduplicate identical payloads across directories, atomically compress archives into ZIP format, persist metadata catalogs, and stream live execution events over Server-Sent Events (SSE).

---

## Why ThreadVault?

Naive backup utilities (e.g. recursive copy scripts) suffer from fundamental engineering bottlenecks:
1. **Redundant I/O and Disk Saturation**: Re-copying unmodified files wastes bandwidth and degrades storage media.
2. **Storage Bloat**: Duplicate files (shared libraries, copied documents, assets) multiply storage footprint linearly.
3. **Thread Contention & Memory Exhaustion**: Creating unbounded threads per file (`new Thread()`) quickly crashes the JVM (`OutOfMemoryError: unable to create native thread`) on large directory trees.
4. **Data Corruption Under Concurrency**: Concurrent disk writes without synchronization or atomic moves create partially written, corrupted archives.
5. **Lack of Real-Time Visibility**: Monolithic backup operations block silently without observable progress.

ThreadVault solves these challenges using **bounded Producer–Consumer concurrency**, **content-addressable deduplication**, **atomic filesystem operations**, and **non-blocking SSE progress streaming**.

---

## Key Features

- **Producer–Consumer Concurrency**: Decouples directory scanning from compression using bounded `ArrayBlockingQueue<FileTask>` queues and custom worker thread pools.
- **Content-Based Deduplication**: SHA-256 cryptographic hashing ensures identical data across different directories is compressed and stored only once (`backup_storage/<hash>.zip`).
- **Atomic Incremental Backups**: $\mathcal{O}(1)$ last-modified timestamp and file size checks skip unmodified files before hashing or compression.
- **Atomic ZIP Compression**: Temporary-file writing (`.tmp.<uuid>`) with atomic filesystem moves (`ATOMIC_MOVE`) prevents concurrent readers from observing incomplete archives.
- **Real-Time Observability**: Live progress broadcasting via Server-Sent Events (SSE) with automatic subscriber lifecycle cleanup.
- **Strict Path Security**: Enforces canonical destination checks during restore (`toAbsolutePath().normalize().startsWith()`), completely blocking path traversal directory escapes (`../../`).
- **Production-Ready Operations**: Custom Spring Boot Actuator health checks (`/actuator/health`), OpenAPI 3 / Swagger UI (`/swagger-ui.html`), graceful shutdown, SLF4J logging, and multi-stage Docker Compose deployment with persistent volumes.

---

## System Architecture

```text
                     Next.js Web Dashboard
                     (React 19 / Port 3000)
                              │
                              │ REST API / SSE Stream
                              ▼
                     Spring Boot REST API
                         (Port 8080)
                              │
                              ▼
                  Application Service Layer
                 (BackupService, CatalogService)
                              │
                              ▼
                      ThreadVault Core
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
 DirectoryScanner       BackupWorker Pool       MetadataStore
   (Producer)             (Consumers)         (O(1) Concurrent Index)
        │                     │                     │
        └──────────────┬──────┴─────────────────────┘
                       │
                       ▼
        Incremental Check ──► SHA-256 Hash ──► Deduplication ──► Atomic ZIP
                       │
                       ▼
              Backup Storage & Catalog
               (Persistent Volumes)
```

---

## How It Works

```text
User / REST Request
        ↓
BackupController (Validates input, returns 202 Accepted with BackupJob ID)
        ↓
BackupServiceImpl (Submits async task to background job executor)
        ↓
DirectoryScanner (Walks directory tree, pushes FileTasks into bounded ArrayBlockingQueue)
        ↓
BackupWorker Pool (Pulls tasks concurrently from queue)
   ├── Step 1: Incremental Check (Matches timestamp & size against MetadataStore; skips if unchanged)
   ├── Step 2: Cryptographic Hash (Computes SHA-256 digest in 8KB buffer chunks)
   ├── Step 3: Deduplication (Checks in-memory hash index; if duplicate, skips compression & reuses archive)
   ├── Step 4: Atomic ZIP (Writes Deflate stream to .tmp.<uuid>, atomically renames to <hash>.zip)
   └── Step 5: Metadata Enqueue (Pushes FileMetadata to metadata queue)
        ↓
MetadataWriter (Single dedicated thread drains metadata queue and commits to metadata.json)
        ↓
BackupEventHub (Broadcasts real-time progress events to connected SSE client streams)
```

---

## Concurrency Model

### Why Producer–Consumer with `ArrayBlockingQueue`?
Directory traversal (I/O metadata lookup) and file compression (CPU-bound hashing + deflate compression) have vastly different execution profiles. Producer–Consumer decouples the scanner from workers via a bounded memory buffer:
- **Automatic Backpressure**: If worker threads are busy compressing large files, `fileQueue.put(task)` automatically blocks the `DirectoryScanner` thread, capping memory usage regardless of directory depth.
- **Thread Safety**: Eliminates manual synchronization between scanner and workers.
- **Graceful Termination**: Uses sentinel **poison pills** to cleanly shut down worker threads when scanning finishes.

### Why NOT Create One Thread Per File?
Spawning a thread per file (`new Thread(...)`) fails rapidly:
1. **Thread Exhaustion**: Traversing 50,000 files attempts to allocate 50,000 OS threads, crashing the JVM with `OutOfMemoryError: unable to create native thread`.
2. **Context Switching Overhead**: Thousands of competing threads saturate OS schedulers and CPU caches.
3. **Disk I/O Thrashing**: Thousands of concurrent uncoordinated disk reads destroy sequential read performance on mechanical and flash storage.

---

## Deduplication

ThreadVault implements **Content-Addressable Storage (CAS)**:
- Files are addressed by their cryptographic digest: `backup_storage/<sha256>.zip`.
- If two files in different directories (`/docs/report.pdf` and `/shared/copy.pdf`) have identical bytes, both compute the same SHA-256 hash.
- The worker compresses the content **once** into `<sha256>.zip`.
- Both files register individual `FileMetadata` records pointing to the shared `<sha256>.zip` archive.
- During restore, the engine extracts the shared archive to both distinct target paths, guaranteeing byte-for-byte SHA-256 fidelity.

---

## Incremental Backup

ThreadVault uses a cheap $\mathcal{O}(1)$ metadata comparison before hashing:
1. Extract `size` and `lastModifiedTime` (in milliseconds from filesystem metadata).
2. Query in-memory `MetadataStore` by canonical original path.
3. If entry exists and both `lastModifiedTime` and `size` match: **skip file immediately** without opening, hashing, or compressing.
4. If modified or new: proceed to SHA-256 hashing, deduplication, and atomic compression.

---

## Performance Benchmarks

All metrics below represent **actual measured numbers** executed on macOS (`aarch64` Apple Silicon, 8 cores, OpenJDK 25, 2GB heap) using cold temporary directories. Results represent the arithmetic mean across **3 independent test runs**:

### 1. Workload Performance & Storage Efficiency

| Workload | Total Files | Total Input Size | Stored Archive Size | Files Deduplicated | Execution Time (4 Workers) | Throughput | Deduplication Ratio | Storage Saved |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| **Small Dataset** | 1,000 | 7.67 MB | 7.81 MB | 0 | **1,058 ms** (~1.1 s) | **7.3 MB/s** | 0.0% | -1.8%* |
| **Medium Dataset** | 10,000 | 53.80 MB | 55.19 MB | 0 | **80,090 ms** (~80.1 s) | **0.7 MB/s** | 0.0% | -2.6%* |
| **Duplicate-Heavy** | 10,000 | 68.13 MB | 20.85 MB | 7,000 | **63,004 ms** (~63.0 s) | **1.1 MB/s** | **70.0%** | **69.4%** |

*\*Note: For incompressible small random binary files (< 5KB), ZIP archive metadata headers add a minor overhead (~2%), which is accurately reflected in real benchmarks.*

---

### 2. Concurrency Speedup (Sequential vs Multi-Worker)

| Workload | Total Files | Sequential (1 Worker) | Concurrent (4 Workers) | Concurrent (8 Workers) | Speedup (4 Workers) | Speedup (8 Workers) |
|---|---:|---:|---:|---:|---:|---:|
| **Small Dataset** | 1,000 | 2,397 ms (~2.4 s) | 1,058 ms (~1.1 s) | 1,002 ms (~1.0 s) | **2.27x** | **2.39x** |
| **Medium Dataset** | 10,000 | 90,240 ms (~90.2 s) | 80,090 ms (~80.1 s) | 69,759 ms (~69.8 s) | **1.13x** | **1.29x** |
| **Duplicate-Heavy** | 10,000 | 84,393 ms (~84.4 s) | 63,004 ms (~63.0 s) | 65,936 ms (~65.9 s) | **1.34x** | **1.28x** |

### Benchmark Metric Formulas
- **Throughput**: $\text{Throughput (MB/s)} = \frac{\text{Total Processed Input MB}}{\text{Elapsed Time (Seconds)}}$
- **Deduplication Ratio**: $\text{Deduplication Ratio (\%)} = \left(\frac{\text{Duplicate Files}}{\text{Total Files}}\right) \times 100$
- **Storage Saved**: $\text{Storage Saved (\%)} = \left(1 - \frac{\text{Total Stored Archive Bytes}}{\text{Total Original Input Bytes}}\right) \times 100$
- **Speedup**: $\text{Speedup Factor} = \frac{\text{Sequential Time (1 Worker)}}{\text{Concurrent Time (N Workers)}}$

*Raw JSON benchmark output is preserved in [`benchmarks/results/latest.json`](benchmarks/results/latest.json).*

---

## Engineering Decisions

| Decision | Implementation Choice | Trade-off Accepted & Justification |
|---|---|---|
| **Pipeline Concurrency** | `ArrayBlockingQueue<FileTask>(100)` | Bounded buffer applies backpressure on the producer scanner, preventing JVM out-of-memory crashes on large directory hierarchies. |
| **Deduplication Key** | SHA-256 (256-bit Digest) | Cryptographically collision-resistant content addressability. Accepts minor CPU hashing overhead (~400 MB/s per core) over insecure MD5/CRC32. |
| **Concurrent Lookup Index** | `ConcurrentHashMap<String, FileMetadata>` | Lock-free $\mathcal{O}(1)$ reads for incremental change checks and deduplication cache lookups across worker threads. |
| **Live Observability** | Server-Sent Events (SSE) | Lightweight unidirectional HTTP streaming over standard port 8080. Avoids the protocol complexity and TCP duplex overhead of WebSockets. |
| **Metadata Persistence** | Single-node JSON (`metadata.json`) | Human-readable, zero external database dependencies (no PostgreSQL/Redis needed). Trade-off: $\mathcal{O}(N)$ memory scaling where $N$ is file count. |
| **Archive Format** | Standalone ZIP (`<hash>.zip`) | Content-addressed Deflate compression. Allows fine-grained per-file restore and cross-run deduplication without monolithic container rewrites. |
| **Atomic File Moves** | `.tmp.<uuid>` + `Files.move(ATOMIC_MOVE)` | Eliminates race conditions where concurrent workers or restore routines read partially compressed archive blobs. |
| **Fixed Worker Pool** | `Executors.newFixedThreadPool(workers)` | Prevents thread explosion and context-switching overhead, matching concurrency directly to available CPU cores and I/O capacity. |
| **Incremental Check Order** | Timestamp & Size Check *before* Hashing | Avoids opening file streams and reading full file bytes into `MessageDigest` when last-modified time and size are identical. |

---

## Failure Handling & Recovery

| Failure Scenario | Current Behavior | Protection Mechanism | Recovery Action |
|---|---|---|---|
| **Worker I/O Exception** | Worker catches exception, increments `failedFiles` counter. | Isolated `try/catch` block inside `BackupWorker.processFile()`. | Remaining files in queue continue processing; job status marks partial/complete with error logs. |
| **Application Crash Mid-Compression** | Temporary file `.tmp.<uuid>` remains unrenamed in storage. | Atomic rename ensures only fully finalized archives are named `<hash>.zip`. | On restart, completed archives remain valid; unfinished files are reprocessed automatically. |
| **Disk Full during Backup** | `IOException: No space left on device` thrown during write. | Handled via global error logging in worker and metadata writer. | Worker fails gracefully; partial valid archives and metadata written prior to disk exhaustion persist. |
| **Duplicate Files Processed Simultaneously** | Two workers hash identical files concurrently. | `DeduplicationEngine.putIfAbsent(hash, path)` + `AtomicMove(REPLACE_EXISTING)`. | First worker compresses; second reuses archive. Atomic move guarantees final archive integrity. |
| **Metadata Update Failure** | `MetadataWriter` encounters filesystem error saving JSON. | Dedicated single-thread queue consumer with synchronization on `saveMetadata()`. | Retries write; archive blobs remain safe on disk. |
| **Interrupted Backup (Ctrl+C / Kill)** | Spring Boot graceful shutdown drains active workers. | `@PreDestroy` in `BackupServiceImpl` calls `executor.awaitTermination(5, SECONDS)`. | In-flight tasks finalize writing metadata before process exit. |
| **Restore Path Traversal Attack** | Malicious path contains `../../outside.txt`. | `RestoreManager` enforces `baseDir.resolve(rel).normalize().toAbsolutePath().startsWith(baseDir)`. | Throws `SecurityException` and aborts malicious file extraction immediately. |
| **Source File Modified During Backup** | Worker reads file while external process writes. | SHA-256 is computed over exact read bytes; timestamp recorded. | Content is compressed consistently with computed hash; subsequent backup will detect updated timestamp. |

---

## Security

- **Path Traversal Defense**: The restore engine checks canonical path containment before extracting files. Any path resolving outside the designated restore directory triggers an immediate `SecurityException`.
- **Input Validation**: `BackupRequest` and `RestoreRequest` validate that paths cannot be blank and exist on the filesystem before creating background jobs.
- **CORS Whitelist**: Whitelists local frontend origins (`http://localhost:3000`, `http://127.0.0.1:3000`).
- **Non-Root Execution**: Docker containers run as unprivileged system users (`appuser:1001` in backend, `nextjs:1001` in frontend).

---

## Observability

- **Structured SLF4J Logging**: Clean log levels (`INFO` for lifecycle milestones, `DEBUG` for per-file operations, `WARN` for retries, `ERROR` for failures).
- **Spring Boot Actuator Health (`/actuator/health`)**: Custom `ThreadVaultStorageHealthIndicator` checks read/write permissions and free disk space on storage and metadata paths.
- **Server-Sent Events (`/api/backups/{id}/stream`)**: Emits real-time JSON events (`FILE_PROCESSED`, `FILE_DEDUPLICATED`, `FILE_SKIPPED`, `BACKUP_COMPLETED`).
- **Interactive Swagger UI (`/swagger-ui.html`)**: Complete OpenAPI 3 documentation for all endpoints and schemas.

---

## Screenshots & Workflow

### Interactive Terminal CLI
![ThreadVault CLI Backup](assets/ss1.png)
*Interactive CLI: Concurrent worker execution, incremental change detection, and content deduplication.*

![ThreadVault CLI Restore](assets/ss2.png)
*Interactive CLI: Automated directory structure recreation and archive extraction.*

---

## REST API Reference

| Method | Endpoint | Description | Status Code |
|---|---|---|---|
| `POST` | `/api/backups` | Submits an asynchronous backup job | `202 Accepted` |
| `GET` | `/api/backups/{id}` | Retrieves backup job progress, status, and statistics | `200 OK` |
| `GET` | `/api/backups` | Lists all historical and active backup jobs | `200 OK` |
| `GET` | `/api/backups/{id}/stream` | Streams live Server-Sent Events (SSE) progress | `200 OK` (Stream) |
| `POST` | `/api/backups/{id}/restore` | Restores backed-up files from metadata catalog | `200 OK` |
| `GET` | `/api/catalog` | Retrieves storage reduction metrics and deduplication summary | `200 OK` |
| `GET` | `/api/catalog/files` | Queries paginated catalog files with path and hash filters | `200 OK` |
| `GET` | `/actuator/health` | Application and storage subsystem health check | `200 OK` |

---

## Project Structure

```text
ThreadVault/
├── Dockerfile                     # Multi-stage Java 21 backend container
├── docker-compose.yml             # Full-stack Docker Compose configuration
├── pom.xml                        # Maven dependencies (Spring Boot, Actuator, OpenAPI)
├── benchmarks/                    # Performance benchmark results and runner
│   ├── README.md                  # Benchmark methodology and results
│   └── results/latest.json        # Measured raw JSON benchmark metrics
├── docs/                          # Comprehensive technical documentation
│   └── THREADVAULT_ENGINEERING_HANDBOOK.md  # 34-section Senior/Interview handbook
├── frontend/                      # Next.js 16 Web Dashboard
│   ├── Dockerfile                 # Multi-stage Node.js container
│   ├── app/                       # Next.js App Router pages (Dashboard, Backups, Catalog, Restore)
│   ├── components/                # Modular React UI components
│   └── lib/sse/                   # Custom useBackupStream SSE hook
└── src/
    ├── main/java/
    │   ├── Main.java              # Application entry point (CLI vs Server mode)
    │   ├── backup/                # BackupManager, BackupWorker
    │   ├── scanner/               # DirectoryScanner (Producer)
    │   ├── incremental/           # IncrementalBackupEngine (Timestamp/Size check)
    │   ├── dedup/                 # DeduplicationEngine, HashCalculator (SHA-256)
    │   ├── compression/           # CompressionManager (Atomic ZIP)
    │   ├── metadata/              # MetadataStore, MetadataWriter
    │   ├── restore/               # RestoreManager (Secure extraction)
    │   ├── event/                 # BackupEventHub, BackupEventPublisher (SSE)
    │   ├── service/               # BackupService, CatalogService
    │   └── controller/            # BackupController, CatalogController, Actuator
    └── test/java/                 # 31 unit, integration, and concurrency test suites
```

---

## Running Locally

### Prerequisites
- **Java 21+**
- **Maven 3.9+**
- **Node.js 20+**

### 1. Build and Run Backend
```bash
# Compile and package the JAR
mvn clean package

# Option A: Run Interactive CLI Mode (Default)
java -jar target/ThreadVault-1.0-SNAPSHOT.jar

# Option B: Run Spring Boot REST API & SSE Server
java -jar target/ThreadVault-1.0-SNAPSHOT.jar --server
```

### 2. Run Next.js Frontend Dashboard
```bash
cd frontend
npm install
npm run dev
```
*(Access the dashboard at `http://localhost:3000`)*

---

## Running with Docker Compose (Recommended)

```bash
# Build and start all containers in the background
docker compose up --build -d

# Check container status and health
docker compose ps
```
- **Web Dashboard**: [http://localhost:3000](http://localhost:3000)
- **REST API Backend**: [http://localhost:8080](http://localhost:8080)
- **OpenAPI / Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Actuator Health Check**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

### Persistent Named Volumes
- `threadvault_storage` $\rightarrow$ `/app/backup_storage` (Preserves ZIP archives across container recreation)
- `threadvault_metadata` $\rightarrow$ `/app/metadata` (Preserves `metadata.json` catalog across container recreation)

---

## Running Tests & Benchmarks

```bash
# 1. Run Complete Automated Backend Test Suite (31 unit & integration tests)
mvn clean test

# 2. Run Automated Performance Benchmarks (Small, Medium, Duplicate-Heavy)
mvn test-compile
mvn dependency:build-classpath -Dmdep.outputFile=target/cp.txt
java -cp "$(cat target/cp.txt):target/classes:target/test-classes" benchmark.BackupBenchmarkRunner

# 3. Run Frontend Linter & Production Build
cd frontend
npm run lint
npm run build
```

---

## Configuration

| Environment Variable | Default Value | Description |
|---|---|---|
| `PORT` / `SERVER_PORT` | `8080` | HTTP port for the Spring Boot REST/SSE API |
| `THREADVAULT_DEFAULT_WORKERS` | `4` | Default worker thread count for parallel processing |
| `THREADVAULT_STORAGE_PATH` | `backup_storage` | Filesystem path where ZIP archive blobs are stored |
| `THREADVAULT_METADATA_PATH` | `metadata` | Filesystem directory storing `metadata.json` |
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080` | Backend API URL used by the Next.js frontend |

---

## Design Trade-offs & Limitations

1. **In-Memory Job Registry**: `BackupJobRegistry` stores active/completed job execution objects in memory. While the physical backup catalog (`metadata.json`) persists across restarts, historical job execution objects (e.g. `startedAt`, `durationMs`) reset upon server restart.
2. **Single-Node In-Memory Catalog**: `MetadataStore` loads `metadata.json` into memory. Suitable for tens of thousands of files; datasets with millions of files would require an embedded database (e.g., RocksDB or SQLite).
3. **No Encryption at Rest**: Archive blobs are compressed with standard Deflate without AES-256 payload encryption.

---

## Future Improvements

### Near-Term
- Add selective single-file restore from the web UI.
- Add AES-256 encryption-at-rest for archive ZIP files.
- Add persistent SQLite storage for historical backup execution logs.

### Large-Scale / Distributed
- Support AWS S3 / MinIO object storage as destination backends.
- Implement distributed worker coordination via Kafka or gRPC.

---

## Documentation

- **[ThreadVault Engineering & Interview Handbook](docs/THREADVAULT_ENGINEERING_HANDBOOK.md)**: Exhaustive 34-section technical breakdown covering Producer–Consumer architecture, concurrency mechanics, deduplication algorithms, restore safety, failure scenarios, performance benchmarks, and comprehensive interview preparation Q&As.
- **[Interactive OpenAPI 3 / Swagger Documentation](http://localhost:8080/swagger-ui.html)**: Live REST API contract explorer.
- **[Benchmark Methodology & Results](benchmarks/README.md)**: Detailed benchmark setup and reproducibility guide.

---

## License

Apache 2.0 License. © 2026 ThreadVault Engineering.
