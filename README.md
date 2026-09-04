# ThreadVault

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen.svg)
![Next.js](https://img.shields.io/badge/Next.js-16.3-black.svg)
![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)
![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0%20%7C%20Swagger-green.svg)
![Architecture](https://img.shields.io/badge/Architecture-Producer--Consumer-success.svg)

**ThreadVault** is a concurrent incremental backup engine built with **Java 21**, with a **Spring Boot REST API** and a **Next.js dashboard** on top.

The main idea was to build a backup system that doesn't simply copy every file every time. ThreadVault checks whether a file has actually changed, deduplicates identical files using SHA-256, compresses data concurrently, and keeps enough metadata to restore the original directory structure.

I also wanted to use the project to work through some practical systems problems: **bounded concurrency, backpressure, concurrent data structures, atomic file operations, failure handling, and real-time progress reporting**.

---

## Why I Built This

A simple recursive copy works for a small folder, but it starts running into problems as the number of files grows.

For example:

* Copying unchanged files wastes disk I/O.
* The same file stored in multiple directories takes up storage multiple times.
* Creating a thread for every file doesn't scale.
* Multiple workers writing the same archive can leave behind corrupted or partial files.
* A long-running backup is difficult to monitor if there is no progress information.

ThreadVault is my attempt at addressing these problems with a relatively small, understandable architecture.

The core ideas are:

```text
Incremental checks
      ↓
Bounded Producer-Consumer pipeline
      ↓
SHA-256 hashing
      ↓
Content-based deduplication
      ↓
Atomic ZIP creation
      ↓
Metadata persistence
      ↓
Live progress events
```

---

## What It Does

ThreadVault supports:

* Incremental backups
* Concurrent file processing
* Content-based deduplication
* ZIP compression
* Directory restoration
* Real-time backup progress through SSE
* Backup history and catalog information
* REST APIs
* CLI execution
* Next.js web dashboard
* Docker Compose deployment

There is also a separate benchmark suite for testing how the backup pipeline behaves with different workloads.

---

## Architecture

```text
                    Next.js Dashboard
                       React / SSE
                           │
                           │ REST + SSE
                           ▼
                    Spring Boot API
                           │
                           ▼
                    Service Layer
                           │
                           ▼
                    ThreadVault Core
                           │
             ┌─────────────┼─────────────┐
             ▼             ▼             ▼
       DirectoryScanner  Workers     MetadataStore
          Producer      Consumers
             │             │             │
             └─────────────┼─────────────┘
                           │
                           ▼
                  Incremental Check
                           │
                           ▼
                      SHA-256
                           │
                           ▼
                    Deduplication
                           │
                           ▼
                    Atomic ZIP
                           │
                           ▼
                 Backup Storage
```

### Request / Backup Flow

A backup request doesn't process every file directly inside the HTTP request.

Instead:

```text
POST /api/backups
        ↓
BackupController
        ↓
BackupService
        ↓
Background Backup Job
        ↓
DirectoryScanner
        ↓
Bounded FileTask Queue
        ↓
Worker Pool
        ↓
Incremental Check
        ↓
SHA-256 + Deduplication
        ↓
Atomic ZIP
        ↓
MetadataWriter
        ↓
SSE Progress Events
```

This allows the API to return quickly while the actual backup continues in the background.

---

# The Interesting Parts

## 1. Producer-Consumer Concurrency

The backup pipeline uses a bounded `ArrayBlockingQueue<FileTask>` between the directory scanner and the worker threads.

The scanner acts as the **producer**, while the backup workers are the **consumers**.

```text
DirectoryScanner
      │
      │ FileTask
      ▼
┌─────────────────────┐
│ ArrayBlockingQueue  │
│      capacity=100   │
└─────────────────────┘
      │
      ├── Worker 1
      ├── Worker 2
      ├── Worker 3
      └── Worker 4
```

### Why a bounded queue?

Without a limit, a scanner could discover files much faster than workers can process them. That would mean accumulating a large number of tasks in memory.

With a bounded queue, when the workers fall behind, the producer eventually blocks on:

```java
fileQueue.put(task);
```

This gives the system natural **backpressure**.

The scanner doesn't need to know how fast the workers are. The queue handles that coordination.

### Why not create one thread per file?

Suppose a directory contains 50,000 files.

Creating 50,000 threads would cause:

* Huge memory usage
* Excessive context switching
* Thread creation overhead
* More contention for disk I/O
* Potential JVM failure from native thread exhaustion

Instead, ThreadVault uses a fixed worker pool:

```java
Executors.newFixedThreadPool(workers);
```

The number of workers can therefore be controlled independently of the number of files.

---

## 2. Incremental Backups

Before reading the contents of a file, ThreadVault checks whether the file appears unchanged.

For each file, it first looks at:

```text
File size
Last modified timestamp
```

The flow is:

```text
File discovered
      ↓
Look up previous metadata
      ↓
Same size + same timestamp?
      │
   ┌──┴──┐
  YES    NO
   │      │
   ▼      ▼
 SKIP   SHA-256
          ↓
      Deduplicate
          ↓
       Compress
```

This is useful because hashing a large file requires reading the entire file.

If the metadata says the file hasn't changed, ThreadVault can skip that work entirely.

---

## 3. Content-Based Deduplication

ThreadVault uses SHA-256 as the content identifier for stored files.

For example:

```text
/docs/report.pdf
/shared/report-copy.pdf
```

If both files contain exactly the same bytes, they produce the same SHA-256 hash.

Instead of creating two archives:

```text
backup_storage/
├── abc123.zip
└── def456.zip
```

ThreadVault stores the content once:

```text
backup_storage/
└── <sha256>.zip
```

The metadata for both original paths points to that same archive.

During restore, the same archive can therefore be extracted to both original locations.

This separates **where a file came from** from **where its content is stored**.

---

## 4. Atomic Compression

Multiple workers can be processing files at the same time, so a worker should never expose an incomplete archive as if it were finished.

ThreadVault first writes to a temporary file:

```text
<hash>.zip.tmp.<uuid>
```

Once compression finishes, it moves the temporary file to its final name using an atomic filesystem move where supported:

```text
temporary file
      ↓
complete ZIP
      ↓
ATOMIC_MOVE
      ↓
<sha256>.zip
```

This means readers don't see a half-written `<sha256>.zip`.

It also makes recovery after a crash much simpler: finalized archives remain valid, while unfinished temporary files can be ignored or cleaned up.

---

## 5. Concurrent Deduplication

Two workers can theoretically encounter identical files at almost the same time.

Both might calculate:

```text
SHA-256 = abc123...
```

The deduplication index uses a `ConcurrentHashMap` and atomic insertion logic so workers can safely coordinate around the same content hash.

The important part here isn't just using a concurrent collection—it is making sure the **check and insertion don't become a race condition** when multiple workers reach the same hash simultaneously.

---

## 6. Real-Time Progress with SSE

Backups can take a while, so the frontend shouldn't have to repeatedly poll the server just to find out what is happening.

ThreadVault uses **Server-Sent Events (SSE)**.

The browser opens a stream:

```text
GET /api/backups/{id}/stream
```

The backend can then send events such as:

```text
FILE_PROCESSED
FILE_DEDUPLICATED
FILE_SKIPPED
BACKUP_COMPLETED
```

The dashboard can update its progress without repeatedly making status requests.

SSE was a good fit here because the communication is primarily **server → client**.

---

# Performance

I included a small benchmark suite to see how the system behaves under different workloads.

The benchmarks were run on:

* macOS
* Apple Silicon (`aarch64`)
* 8 CPU cores
* OpenJDK 25
* 2 GB heap
* 3 independent runs per workload

The numbers below are averages across those runs.

### Backup Performance

| Workload        |  Files |    Input |   Stored | Deduplicated | 4 Workers | Throughput |
| --------------- | -----: | -------: | -------: | -----------: | --------: | ---------: |
| Small           |  1,000 |  7.67 MB |  7.81 MB |           0% |    1.06 s |   7.3 MB/s |
| Medium          | 10,000 | 53.80 MB | 55.19 MB |           0% |   80.09 s |   0.7 MB/s |
| Duplicate-heavy | 10,000 | 68.13 MB | 20.85 MB |          70% |   63.00 s |   1.1 MB/s |

The duplicate-heavy workload is particularly useful for showing what the deduplication layer is doing: **7,000 of 10,000 files were duplicates**, reducing stored data by about **69.4%**.

### Worker Comparison

| Workload        | 1 Worker | 4 Workers | 8 Workers | 4x Speedup | 8x Speedup |
| --------------- | -------: | --------: | --------: | ---------: | ---------: |
| Small           |   2.40 s |    1.06 s |    1.00 s |      2.27x |      2.39x |
| Medium          |  90.24 s |   80.09 s |   69.76 s |      1.13x |      1.29x |
| Duplicate-heavy |  84.39 s |   63.00 s |   65.94 s |      1.34x |      1.28x |

The results also show an important point: **more threads don't automatically mean proportionally better performance**.

Once disk I/O and the rest of the pipeline become the bottleneck, adding workers gives diminishing returns.

The raw benchmark output is available in:

```text
benchmarks/results/latest.json
```

---

# Failure Handling

A backup system needs to deal with failures without taking down the entire job.

Some of the cases handled by ThreadVault include:

| Situation                               | Behavior                                                             |
| --------------------------------------- | -------------------------------------------------------------------- |
| File I/O error                          | Individual file fails while remaining work continues                 |
| Crash during compression                | Temporary archive is never exposed as a completed archive            |
| Disk full                               | Write failure is recorded and processing can continue where possible |
| Same file processed by multiple workers | Concurrent deduplication prevents duplicate final archives           |
| Metadata write failure                  | Metadata persistence is isolated to its writer                       |
| Application shutdown                    | Active workers are given time to finish                              |
| Malicious restore path                  | Path traversal is rejected                                           |
| Source file changes during backup       | Hash is based on the bytes actually read                             |

The goal is not to pretend failures won't happen, but to make sure one failure doesn't unnecessarily invalidate the entire backup.

---

# Restore Security

Restoration is one of the places where filesystem applications need to be particularly careful.

A malicious archive or metadata entry should not be able to escape the selected restore directory using paths such as:

```text
../../outside.txt
```

Before extracting a file, ThreadVault resolves and normalizes the destination path and verifies that it still belongs under the intended restore directory.

Conceptually:

```text
restore root
    │
    ├── documents/
    │     └── report.pdf     ✓
    │
    └── ../../outside.txt    ✗
```

An invalid path results in a `SecurityException` rather than writing outside the restore location.

---

# Metadata

The backup engine keeps a metadata catalog containing information about the original files and their stored content.

A simplified relationship looks like:

```text
Original Path
     │
     ├── size
     ├── lastModified
     └── contentHash
              │
              ▼
       <sha256>.zip
```

The metadata catalog is currently stored in:

```text
metadata/metadata.json
```

This keeps the project simple and makes the catalog easy to inspect without requiring an external database.

The trade-off is that the catalog is loaded into memory, so this approach would need to change for very large datasets.

---

# Observability

ThreadVault provides a few ways to see what the application is doing:

### Logging

SLF4J logging is used for:

* Backup lifecycle events
* Worker activity
* Retries
* Failures

### Health Check

Spring Boot Actuator exposes:

```text
GET /actuator/health
```

The custom storage health check verifies things such as storage accessibility and available disk space.

### SSE

Live backup events are exposed through:

```text
GET /api/backups/{id}/stream
```

### Swagger

The REST API is documented through OpenAPI / Swagger UI.

---

# REST API

| Method | Endpoint                    | Description                      |
| ------ | --------------------------- | -------------------------------- |
| `POST` | `/api/backups`              | Start a backup                   |
| `GET`  | `/api/backups/{id}`         | Get backup progress and status   |
| `GET`  | `/api/backups`              | List backup jobs                 |
| `GET`  | `/api/backups/{id}/stream`  | Stream live backup events        |
| `POST` | `/api/backups/{id}/restore` | Restore a backup                 |
| `GET`  | `/api/catalog`              | Get catalog and storage metrics  |
| `GET`  | `/api/catalog/files`        | Query catalog files              |
| `GET`  | `/actuator/health`          | Check application/storage health |

A backup request returns `202 Accepted` because the actual backup runs asynchronously.

---

# Project Structure

```text
ThreadVault/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
│
├── benchmarks/
│   ├── README.md
│   └── results/
│       └── latest.json
│
├── docs/
│   └── THREADVAULT_ENGINEERING_HANDBOOK.md
│
├── frontend/
│   ├── Dockerfile
│   ├── app/
│   ├── components/
│   └── lib/
│       └── sse/
│
└── src/
    ├── main/java/
    │   ├── backup/
    │   ├── scanner/
    │   ├── incremental/
    │   ├── dedup/
    │   ├── compression/
    │   ├── metadata/
    │   ├── restore/
    │   ├── event/
    │   ├── service/
    │   └── controller/
    │
    └── test/java/
```

The core packages are separated by responsibility rather than putting all of the backup logic into one service.

---

# Tech Stack

| Area             | Technology                            |
| ---------------- | ------------------------------------- |
| Language         | Java 21                               |
| Backend          | Spring Boot 3.4.2                     |
| Frontend         | Next.js 16 / React                    |
| Database         | JSON-based metadata catalog           |
| API              | REST + Server-Sent Events             |
| Documentation    | OpenAPI / Swagger                     |
| Concurrency      | Java Executors + `ArrayBlockingQueue` |
| Hashing          | SHA-256                               |
| Compression      | ZIP / Deflate                         |
| Containerization | Docker Compose                        |
| Testing          | JUnit                                 |

---

# Running Locally

## Prerequisites

* Java 21+
* Maven 3.9+
* Node.js 20+

## Backend

Build the project:

```bash
mvn clean package
```

Run the CLI:

```bash
java -jar target/ThreadVault-1.0-SNAPSHOT.jar
```

Or start the REST API:

```bash
java -jar target/ThreadVault-1.0-SNAPSHOT.jar --server
```

The backend runs on:

```text
http://localhost:8080
```

## Frontend

```bash
cd frontend
npm install
npm run dev
```

The dashboard runs on:

```text
http://localhost:3000
```

---

# Docker Compose

Docker Compose is the easiest way to run the complete application.

```bash
docker compose up --build -d
```

Check the containers:

```bash
docker compose ps
```

The main services are available at:

```text
Dashboard
http://localhost:3000

REST API
http://localhost:8080

Swagger UI
http://localhost:8080/swagger-ui.html

Health Check
http://localhost:8080/actuator/health
```

Backup data is stored in persistent Docker volumes:

```text
threadvault_storage
    → /app/backup_storage

threadvault_metadata
    → /app/metadata
```

---

# Testing & Benchmarks

Run the backend tests with:

```bash
mvn clean test
```

Run the benchmark suite:

```bash
mvn test-compile

mvn dependency:build-classpath \
    -Dmdep.outputFile=target/cp.txt

java -cp "$(cat target/cp.txt):target/classes:target/test-classes" \
    benchmark.BackupBenchmarkRunner
```

Build and lint the frontend:

```bash
cd frontend

npm run lint
npm run build
```

---

# Configuration

| Variable                      | Default                 | Purpose                  |
| ----------------------------- | ----------------------- | ------------------------ |
| `PORT` / `SERVER_PORT`        | `8080`                  | REST/SSE server port     |
| `THREADVAULT_DEFAULT_WORKERS` | `4`                     | Number of backup workers |
| `THREADVAULT_STORAGE_PATH`    | `backup_storage`        | Archive storage location |
| `THREADVAULT_METADATA_PATH`   | `metadata`              | Metadata directory       |
| `NEXT_PUBLIC_API_BASE_URL`    | `http://localhost:8080` | Backend URL for frontend |

---

# Design Trade-offs

ThreadVault intentionally keeps some parts simple.

### JSON instead of a database

The metadata catalog is stored in JSON because it keeps the project self-contained and easy to inspect.

The downside is that loading the entire catalog into memory won't scale indefinitely.

For a much larger system, I'd move this to something like SQLite, RocksDB, or another persistent indexed store.

### ZIP instead of a custom archive format

ZIP is well understood, easy to inspect, and supports individual file extraction.

A custom archive format could potentially offer better control, but it would add complexity without much benefit for this project.

### SSE instead of WebSockets

The dashboard mostly needs information flowing from the server to the browser.

SSE provides that without introducing the additional complexity of a bidirectional WebSocket connection.

### In-memory job registry

Backup execution state is currently kept in memory.

The actual backup files and metadata survive a restart, but historical runtime information such as job duration does not.

---

# Limitations

There are a few areas I would change if this were being built for a much larger production environment:

* Metadata is currently backed by JSON and loaded into memory.
* Backup job history is not persisted separately from the backup catalog.
* Archive encryption is not implemented.
* The system currently assumes a single application node.
* Storage is local filesystem based.
* Distributed worker coordination is not supported.

---

# Future Improvements

Some things I'd like to explore next:

### Near term

* Single-file restore from the web dashboard
* AES-256 encryption for stored archives
* Persistent backup job history
* Better cleanup of abandoned temporary files
* More detailed dashboard metrics

### Larger-scale version

* S3 / MinIO storage backends
* SQLite or RocksDB metadata storage
* Distributed worker processing
* Kafka or gRPC-based job coordination
* Multi-node deployment
* More extensive failure and recovery testing

---

# Documentation

* Swagger UI — interactive REST API documentation when the application is running.

---

# License

Apache 2.0 License.

© 2026 ThreadVault Engineering
