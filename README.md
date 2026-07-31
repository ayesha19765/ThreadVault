# ThreadVault

[![Java](https://img.shields.io/badge/Java-25-orange)](https://www.oracle.com/java/)
[![Build](https://img.shields.io/badge/Build-Maven-blue)](https://maven.apache.org/)
[![Concurrency](https://img.shields.io/badge/Architecture-Producer--Consumer-success)]()
[![Storage](https://img.shields.io/badge/Storage-Incremental%20%7C%20Deduplicated-blueviolet)]()

A concurrent backup engine built in Java that explores the core techniques used in modern backup systems. ThreadVault combines incremental backups, content-based deduplication, compression, metadata indexing, scheduling, and real-time directory monitoring into a modular, multithreaded architecture.

Instead of repeatedly processing every file, ThreadVault identifies unchanged content, skips unnecessary work, and stores duplicate data only once, resulting in faster backups and reduced storage usage.

---

## Highlights

- Concurrent backup pipeline using the Producer–Consumer pattern
- Incremental backups based on file metadata
- SHA-256 content-based deduplication
- ZIP compression for storage optimization
- Metadata-driven restore workflow
- Scheduled backups with configurable intervals
- Real-time directory monitoring using Java WatchService
- Thread-safe implementation using Java Concurrency utilities

---

## System Architecture

```text
                          BackupCLI
                              │
                              ▼
                      BackupManager
                              │
                              ▼
                    DirectoryScanner
                              │
                              ▼
                BlockingQueue<FileTask>
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
     Worker-1             Worker-2             Worker-N
         │                    │                    │
         └────────────────────┼────────────────────┘
                              ▼
                   Incremental Backup Check
                              │
                              ▼
                       SHA-256 Hashing
                              │
                              ▼
                    Deduplication Engine
                              │
                              ▼
                     ZIP Compression
                              │
                              ▼
                      Backup Repository
                              │
                              ▼
                       Metadata Store
```

The backup workflow is organized as a processing pipeline. A directory scanner discovers files and submits them to a shared `BlockingQueue`. Worker threads consume tasks concurrently, perform incremental checks, calculate content hashes, eliminate duplicate data, compress unique files, and update the metadata index. This separation of responsibilities keeps the system scalable, modular, and thread-safe.

---

## Key Features

### Concurrent Processing

Files are processed in parallel using a fixed-size worker pool backed by Java's `ExecutorService`. The Producer–Consumer design allows file discovery and backup operations to execute independently, improving throughput while maintaining predictable resource usage.

### Incremental Backup

Before performing expensive operations such as hashing or compression, ThreadVault compares the current file's metadata with the previous backup. Files whose path, size, and last modified timestamp remain unchanged are skipped immediately.

### Content-Based Deduplication

Every processed file is identified using a SHA-256 hash. If identical content already exists in the backup repository, the file is not stored again. Instead, metadata references the existing backup, reducing storage requirements without affecting restore operations.

### Compression

Unique files are compressed into ZIP archives before being written to disk. Each archive is stored using its content hash, providing a deterministic and collision-resistant storage layout.

### Metadata-Driven Restore

All backup information is maintained in a persistent metadata index. During restoration, the application locates the required archive through metadata rather than scanning the backup repository, making restores predictable and efficient.

---

## How It Works

Every backup follows the same processing pipeline. Each stage performs a single responsibility, making the system easier to maintain and extend.

```text
File Detected
      │
      ▼
Incremental Check
      │
      ▼
SHA-256 Hashing
      │
      ▼
Deduplication
      │
      ▼
ZIP Compression
      │
      ▼
Metadata Update
      │
      ▼
Backup Complete
```

Only files that require processing move through the complete pipeline. Files that have not changed since the previous backup are skipped before any expensive operations are performed.

---

### Incremental Backup

ThreadVault minimizes unnecessary work by comparing each file against previously stored metadata before processing.

A file is considered unchanged if all of the following attributes match:

- Relative file path
- File size
- Last modified timestamp

```text
                 File Detected
                       │
                       ▼
          Compare with Stored Metadata
                       │
              ┌────────┴────────┐
              ▼                 ▼
         Unchanged          Modified
              │                 │
              ▼                 ▼
        Skip File         Continue Pipeline
```

By avoiding redundant hashing, compression, and writes, incremental backups become significantly faster when only a small portion of the dataset changes.

---

### Content-Based Deduplication

Two files may have different names while containing identical data.

ThreadVault computes a SHA-256 hash for every processed file and uses it as a unique identifier.

```text
report.pdf
SHA-256
c4ab1...

copy.pdf
SHA-256
c4ab1...
```

Since both files produce the same hash, only one compressed archive is stored.

The metadata index keeps track of every original file that references the archived content.

This approach reduces storage usage without affecting file restoration.

---

### Compression

After deduplication, unique files are compressed before being written to the backup repository.

```text
Original File
      │
      ▼
ZIP Compression
      │
      ▼
Compressed Archive
      │
      ▼
Backup Repository
```

Archives are stored using their SHA-256 hash as the filename.

```text
backup_storage/

├── 15b6e7....zip
├── 4cb2f1....zip
├── a18f90....zip
└── ...
```

Using content hashes as storage identifiers guarantees that identical files always map to the same archive.

---

### Metadata Management

Metadata acts as the central index for the entire backup system.

Each entry stores:

- Original file path
- SHA-256 hash
- Backup archive location
- Original file size
- Compressed file size
- Last modified timestamp
- Backup timestamp

Metadata is persisted as JSON, allowing the application to:

- Detect unchanged files
- Identify duplicate content
- Locate backup archives
- Restore files efficiently

```text
metadata/

└── metadata.json
```

Keeping metadata separate from archived data simplifies restore operations and provides a clear separation between file storage and backup indexing.

---

### Restore Workflow

Restoring a file does not require scanning the backup repository.

Instead, ThreadVault uses the metadata index to locate the corresponding archive directly.

```text
metadata.json
      │
      ▼
Locate Archive
      │
      ▼
Extract ZIP File
      │
      ▼
Restore Original File
```

This metadata-driven workflow keeps restore operations efficient even as the backup repository grows.

---

## Concurrency Model

ThreadVault uses the Producer–Consumer pattern to decouple file discovery from backup processing.

The directory scanner continuously discovers files and submits them to a shared `BlockingQueue`. A fixed-size worker pool consumes these tasks concurrently, allowing multiple files to be processed in parallel while keeping resource usage predictable.

```text
            Directory Scanner
                    │
                    ▼
      BlockingQueue<FileTask>
                    │
     ┌──────────────┼──────────────┐
     ▼              ▼              ▼
 Worker-1       Worker-2       Worker-N
     │              │              │
     └──────────────┼──────────────┘
                    ▼
            Backup Processing
```

This design separates I/O-bound directory traversal from CPU-intensive operations such as hashing and compression, improving throughput while keeping the implementation modular.

### Thread Safety

Shared state is managed using Java's concurrency utilities.

| Component | Purpose |
|----------|---------|
| `BlockingQueue` | Coordinates communication between the scanner and worker threads |
| `ConcurrentHashMap` | Enables concurrent metadata and hash lookups |
| `ExecutorService` | Manages a fixed-size worker pool |
| `AtomicInteger` / `LongAdder` | Maintains thread-safe backup statistics |

The combination of these utilities allows multiple files to be processed concurrently without introducing race conditions or inconsistent metadata.

---

## Project Structure

```text
src
└── main
    ├── java
    │   ├── backup
    │   ├── cli
    │   ├── compression
    │   ├── config
    │   ├── dedup
    │   ├── incremental
    │   ├── metadata
    │   ├── restore
    │   ├── scanner
    │   ├── scheduler
    │   ├── stats
    │   └── watcher
    │
    └── resources
        └── config.properties

backup_storage/
metadata/
restore/
sample_data/
```

The project follows a modular package structure where each component is responsible for a single part of the backup workflow. This separation keeps the codebase easy to navigate and simplifies future extensions.

---

## Built With

| Technology | Purpose |
|-----------|---------|
| Java | Core application development |
| Maven | Build and dependency management |
| Jackson | Metadata serialization and persistence |
| Java NIO | File operations and directory monitoring |
| Java Concurrency API | Multithreading and synchronization |
| ZIP Streams | File compression |

---

## Getting Started

### Prerequisites

- Java 21 or later
- Maven 3.9+

### Clone the repository

```bash
git clone https://github.com/<your-username>/ThreadVault.git
cd ThreadVault
```

### Build

```bash
mvn clean package
```

### Run

```bash
java -jar target/ThreadVault.jar
```

---

## Configuration

Application settings are defined in `src/main/resources/config.properties`.

```properties
workers=4
backup.directory=sample_data
watch.mode=false
```

| Property | Description |
|----------|-------------|
| `workers` | Number of worker threads |
| `backup.directory` | Directory to back up |
| `watch.mode` | Enables real-time directory monitoring |

---

## Sample Output

### Backup Operation

<p align="center">
  <img src="assets/ss1.png" alt="Backup Execution" width="900">
</p>

### Restore Operation

<p align="center">
  <img src="assets/ss2.png" alt="Restore Execution" width="900">
</p>

---

## Future Enhancements

While ThreadVault implements the core building blocks of a modern backup engine, there are several areas that could be explored in future iterations.

- Differential backup support
- Backup versioning
- AES-256 encryption for backup archives
- Cloud storage integration (AWS S3, Azure Blob Storage)
- Backup integrity verification using checksums
- Configurable compression algorithms
- Parallel restore operations
- Web-based monitoring dashboard
- Metrics and monitoring with Prometheus
- Containerized deployment using Docker


---

## Acknowledgements

ThreadVault explores architectural concepts commonly found in modern backup and data protection systems. The project was built as a practical exercise in concurrent programming, storage optimization, and scalable system design using Java.

---

If you found this project interesting or have suggestions for improvements, feel free to open an issue or submit a pull request.

---


Made With Love 🧡

©2026 Ayesha’s ThreadVault. All rights reserved.
