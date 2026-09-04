# ThreadVault

[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-16.3-black)](https://nextjs.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue)](https://www.docker.com/)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0%20%7C%20Swagger-green)](http://localhost:8080/swagger-ui.html)
[![Architecture](https://img.shields.io/badge/Architecture-Producer--Consumer-success)]()

**ThreadVault** is a high-performance, concurrent incremental backup and content-based deduplication engine built with **Java 21**, **Spring Boot**, and **Next.js**. It features a lock-free multithreaded file processing pipeline, SHA-256 content addressability, atomic ZIP archive compression, real-time Server-Sent Events (SSE) progress streaming, path traversal protection, and an interactive web dashboard.

---

## Key Highlights

- **Producer–Consumer Concurrency**: Lock-free task coordination using bounded `ArrayBlockingQueue` queues and custom worker thread pools.
- **Content-Based Deduplication**: SHA-256 cryptographic hashing ensures identical data across different directories is compressed and stored only once.
- **Atomic Incremental Backups**: Fast last-modified-time and size change checks skip unmodified files before hashing or compression.
- **Atomic ZIP Compression**: Temporary-file writing with atomic filesystem moves prevents concurrent readers from observing incomplete archives.
- **Real-Time Observability**: Real-time progress broadcasting via Server-Sent Events (SSE) with automatic subscriber lifecycle cleanup.
- **Strict Path Security**: Path traversal defense ensuring all restored files strictly remain within designated restore roots.
- **Production-Ready Operations**: Spring Boot Actuator health checks (`/actuator/health`), OpenAPI 3 / Swagger UI (`/swagger-ui.html`), graceful shutdown, SLF4J logging, and multi-stage Docker Compose deployment with persistent volumes.

---

## System Architecture

```text
                     Next.js Web Dashboard
                        (Port 3000)
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

## Core Engineering Highlights

| Feature | Implementation Mechanism | Engineering Purpose |
|---|---|---|
| **Pipeline Concurrency** | `ArrayBlockingQueue<FileTask>(100)` & `ExecutorService` | Decouples filesystem directory traversal from CPU/IO-bound compression. |
| **Deduplication** | `ConcurrentHashMap.putIfAbsent(hash, path)` | Prevents duplicate physical storage across multiple files with identical content. |
| **Atomic Writes** | `.tmp.<uuid>` temp file + `Files.move(ATOMIC_MOVE)` | Eliminates half-written or corrupted ZIP archives during concurrent execution. |
| **Path Traversal Defense** | `baseDir.resolve(rel).normalize().toAbsolutePath()` | Blocks malicious directory escape attempts (`../../`) with `SecurityException`. |
| **Live Observability** | `SseEmitter` + `BackupEventHub` | Delivers real-time progress events without coupling the backup engine to HTTP. |
| **Storage Health** | Custom `HealthIndicator` (`/actuator/health`) | Verifies filesystem read/write access and available disk space. |

---

## Running with Docker Compose (Recommended)

ThreadVault includes a production-ready, multi-stage Docker Compose configuration:

```bash
# Build and start all services in the background
docker compose up --build -d

# Check service health and logs
docker compose ps
docker compose logs -f
```

- **Web Dashboard**: [http://localhost:3000](http://localhost:3000)
- **REST API Backend**: [http://localhost:8080](http://localhost:8080)
- **OpenAPI / Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Actuator Health Check**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

### Persistent Storage

Docker Compose configures named volumes so backup archives and metadata persist across container restarts:

```text
Host Storage
 ├── threadvault_storage ──► /app/backup_storage (Compressed ZIP archives)
 └── threadvault_metadata ─► /app/metadata       (JSON catalog index)
```

---

## Running Locally (Without Docker)

### Prerequisites
- **Java 21+**
- **Maven 3.9+**
- **Node.js 20+**

### 1. Build and Run the Backend
```bash
# Build the JAR
mvn clean package

# Option A: Run Interactive CLI Mode
java -jar target/ThreadVault-1.0-SNAPSHOT.jar

# Option B: Run Spring Boot REST API & SSE Server
java -jar target/ThreadVault-1.0-SNAPSHOT.jar --server
```

### 2. Run the Next.js Frontend
```bash
cd frontend
npm install
npm run dev
```
*(Access the dashboard at `http://localhost:3000`)*

---

## REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/backups` | Submits an asynchronous backup job (`202 Accepted`) |
| `GET` | `/api/backups/{id}` | Retrieves backup job progress, status, and statistics |
| `GET` | `/api/backups` | Lists all historical and active backup jobs |
| `GET` | `/api/backups/{id}/stream` | Streams live Server-Sent Events (SSE) progress |
| `POST` | `/api/backups/{id}/restore` | Restores backed-up files from metadata catalog |
| `GET` | `/api/catalog` | Retrieves storage reduction metrics and deduplication summary |
| `GET` | `/api/catalog/files` | Queries paginated catalog files with path and hash filters |
| `GET` | `/actuator/health` | Application and storage subsystem health status |

Interactive API documentation and schema models are available at `/swagger-ui.html`.

---

## Testing

```bash
# Run Java Backend Test Suite (31 unit & integration tests)
mvn clean test

# Run Frontend Lint
cd frontend && npm run lint

# Run Frontend Production Build
cd frontend && npm run build
```

---

## Documentation

- **[Interactive OpenAPI 3 / Swagger Documentation](http://localhost:8080/swagger-ui.html)**: Live REST API contract explorer.

---

## License

Apache 2.0 License. © 2026 ThreadVault Engineering.
