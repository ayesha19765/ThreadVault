# ThreadVault

[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/)
[![Build](https://img.shields.io/badge/Build-Maven-blue)](https://maven.apache.org/)
[![Concurrency](https://img.shields.io/badge/Architecture-Producer--Consumer-success)]()
[![Storage](https://img.shields.io/badge/Storage-Incremental%20%7C%20Deduplicated-blueviolet)]()

A high-performance concurrent backup and deduplication engine built with Java 21 and Spring Boot. ThreadVault combines multithreaded file pipelines, incremental change detection, SHA-256 content deduplication, ZIP compression, real-time Server-Sent Events (SSE) progress streaming, and catalog inspection into a modular, production-ready architecture.

---

## Highlights

- **Concurrent Processing**: Multithreaded backup pipeline using Producer–Consumer pattern and fixed worker thread pools.
- **Real-Time Observability**: Live backup progress streaming via Server-Sent Events (SSE) without HTTP coupling in the core engine.
- **Incremental Backups**: Instant change detection skipping unmodified files before hashing or compression.
- **Content-Based Deduplication**: SHA-256 content hashing to ensure identical data is stored exactly once across multiple files.
- **ZIP Compression**: Deterministic, content-addressed storage layout optimizing disk utilization.
- **Catalog Inspection**: Aggregated repository summary metrics and paginated/filtered file catalog APIs.
- **Dual Interface**: Interactive terminal CLI and non-blocking Spring Boot REST API.

---

## System Architecture

```text
                     ┌──────────────┐
                     │    CLI       │
                     └──────┬───────┘
                            │
                     ┌──────▼───────┐
                     │   Service    │
                     │ (Backup,     │
                     │  Catalog)    │
                     └──────┬───────┘
                            │
                     ┌──────▼───────┐
                     │ ThreadVault  │
                     │     Core     │
                     │ (Scanner,    │
                     │  Workers,    │
                     │  Dedup, ZIP) │
                     └──────┬───────┘
                            │
                    ┌───────▼────────┐
                    │ Backup Events  │ (BackupEventPublisher)
                    └───────┬────────┘
                            │
              ┌─────────────┴─────────────┐
              ▼                           ▼
       Job Statistics                SSE Stream
              │                           │
              └─────────────┬─────────────┘
                            ▼
                     Future Frontend
```

### Core Engine Pipeline

```text
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

---

## REST API Reference

ThreadVault exposes a non-blocking REST API on port `8080`.

| Method | Endpoint | Description | Status Code |
|---|---|---|---|
| `POST` | `/api/backups` | Submits a backup request, starts backup asynchronously | `202 Accepted` |
| `GET` | `/api/backups/{id}` | Retrieves backup job status, duration, and statistics | `200 OK` |
| `GET` | `/api/backups` | Retrieves a list of all recent backup jobs | `200 OK` |
| `GET` | `/api/backups/{id}/stream` | Streams real-time Server-Sent Events (SSE) progress | `200 OK` (Stream) |
| `POST` | `/api/backups/{id}/restore` | Restores backed-up files from metadata catalog | `200 OK` |
| `GET` | `/api/catalog` | Retrieves summary storage metrics and deduplication stats | `200 OK` |
| `GET` | `/api/catalog/files` | Queries paginated catalog files with path and hash filters | `200 OK` |

---

### API Examples

#### 1. Start an Asynchronous Backup
```bash
curl -X POST http://localhost:8080/api/backups \
  -H "Content-Type: application/json" \
  -d '{"source": "sample_data", "workers": 4}'
```
**Response (`202 Accepted`)**:
```json
{
  "backupId": "a7b554cb-e08d-4428-9ea9-11bbce16bb72",
  "status": "QUEUED",
  "source": "sample_data",
  "destination": "backup_storage",
  "workers": 4,
  "filesDiscovered": 0,
  "filesProcessed": 0,
  "filesSkipped": 0,
  "filesDeduplicated": 0,
  "filesIncrementalSkipped": 0,
  "filesFailed": 0,
  "originalBytes": 0,
  "storedBytes": 0,
  "spaceSavedPercentage": 0.0,
  "createdAt": "2026-08-31T15:04:06.965274",
  "durationMs": 0
}
```

#### 2. Stream Real-Time Progress (Server-Sent Events)
```bash
curl -N http://localhost:8080/api/backups/a7b554cb-e08d-4428-9ea9-11bbce16bb72/stream
```
**Event Stream Output**:
```text
event:INITIAL_STATE
data:{"backupId":"a7b554cb...","status":"RUNNING","filesDiscovered":10,"filesProcessed":6,...}

id:7b42d3...
event:FILE_PROCESSED
data:{"backupId":"a7b554cb...","type":"FILE_PROCESSED","file":"documents/report.pdf","fileSize":1048576,"filesDiscovered":10,"filesProcessed":7,"storedBytes":419430,"spaceSavedPercentage":60.0}

id:d6de0b...
event:BACKUP_COMPLETED
data:{"backupId":"a7b554cb...","type":"BACKUP_COMPLETED","status":"COMPLETED","filesDiscovered":10,"filesProcessed":10,"storedBytes":524288,"spaceSavedPercentage":50.0,"durationMs":120}
```

#### 3. Inspect Repository Catalog Summary
```bash
curl http://localhost:8080/api/catalog
```
**Response (`200 OK`)**:
```json
{
  "totalFiles": 25,
  "uniqueFiles": 7,
  "totalOriginalBytes": 531494,
  "totalStoredBytes": 522321,
  "deduplicatedBytes": 9173,
  "spaceSavedPercentage": 1.73,
  "totalBackups": 23,
  "lastBackupTime": "2026-08-31T15:03:31.433583"
}
```

#### 4. Query Paginated Catalog Files
```bash
curl "http://localhost:8080/api/catalog/files?path=png&page=0&size=10"
```
**Response (`200 OK`)**:
```json
{
  "content": [
    {
      "originalPath": "sample_data/sample_png_image.png",
      "hash": "76ebf60e48796a7122568cff722b3b56710f88405da3abb1cc4731f1400258c4",
      "backupPath": "backup_storage/76ebf60e48796a7122568cff722b3b56710f88405da3abb1cc4731f1400258c4.zip",
      "originalSize": 365638,
      "compressedSize": 364238,
      "backupTime": "2026-08-31T12:04:46.275116",
      "lastModifiedTime": 1788156714929,
      "deleted": false,
      "deduplicated": false
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "hasMore": false
}
```

#### 5. Restore Backed-Up Files
```bash
curl -X POST http://localhost:8080/api/backups/a7b554cb-e08d-4428-9ea9-11bbce16bb72/restore \
  -H "Content-Type: application/json" \
  -d '{}'
```

---

## Getting Started

### Prerequisites

- Java 21 or later
- Maven 3.9+

### Build

```bash
mvn clean package
```

### Run Modes

#### 1. Interactive CLI Mode (Default)
```bash
java -jar target/ThreadVault-1.0-SNAPSHOT.jar
```

#### 2. Spring Boot Web & REST API Mode
```bash
java -jar target/ThreadVault-1.0-SNAPSHOT.jar --server
```

---

## Concurrency & Thread Safety

| Component | Concurrency Model | Purpose |
|---|---|---|
| `BlockingQueue<FileTask>` | Thread-safe queue | Coordinates producer scanner and consumer workers |
| `ExecutorService` | Fixed worker pool | Parallel file hashing, dedup, and compression |
| `BackupEventHub` | `ConcurrentHashMap` & `CopyOnWriteArrayList` | Non-blocking domain event distribution |
| `SseEmitter` | Spring Async Web | Long-lived streaming with automatic leak-free subscriber cleanup |
| `MetadataStore` | `ConcurrentHashMap` | Fast O(1) concurrent lookups |
| `BackupStatistics` | `AtomicInteger` & `AtomicLong` | Thread-safe metric accumulators |

---

## Future Roadmap: Frontend & Web Dashboard

- React / Next.js Web Dashboard
- Live SSE Backup Visualizer with per-worker progress bars
- Interactive File Catalog Explorer & Search
- Selective One-Click File Restore UI
- Storage & Deduplication Savings Charts

---

Made With Love 🧡

©2026 Ayesha’s ThreadVault. All rights reserved.
