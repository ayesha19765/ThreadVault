# ThreadVault Performance Benchmarks

This directory contains automated, reproducible benchmarking infrastructure and measured performance datasets for ThreadVault.

---

## Benchmark System & Environment

- **Operating System**: macOS (Darwin 24.6.0, `aarch64` Apple Silicon)
- **CPU Cores**: 8 Physical / Efficiency Cores
- **Java Runtime**: OpenJDK 25.0.2 64-Bit Server VM
- **Memory (Max Heap)**: 2,048 MB
- **Storage Subsystem**: Local APFS NVMe SSD

---

## Measured Benchmark Results

All metrics represent the arithmetic mean across **3 independent test iterations** executed with cold temporary directories to prevent cache pollution.

### 1. Workload Performance & Storage Efficiency

| Workload | Total Files | Total Input Size | Stored Archive Size | Files Deduplicated | Execution Time (4 Workers) | Throughput | Deduplication Ratio | Storage Saved |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| **Small Dataset** | 1,000 | 7.67 MB | 7.81 MB | 0 | 1,058 ms (~1.1 s) | 7.3 MB/s | 0.0% | -1.8%* |
| **Medium Dataset** | 10,000 | 53.80 MB | 55.19 MB | 0 | 80,090 ms (~80.1 s) | 0.7 MB/s | 0.0% | -2.6%* |
| **Duplicate-Heavy** | 10,000 | 68.13 MB | 20.85 MB | 7,000 | 63,004 ms (~63.0 s) | 1.1 MB/s | 70.0% | **69.4%** |

*\*Note: For incompressible small random binary files (< 5KB), ZIP archive metadata headers add a minor overhead (~2%), which is accurately reflected in real benchmarks.*

---

### 2. Concurrency Speedup (Sequential vs. Multi-Worker)

| Workload | Total Files | Sequential (1 Worker) | Concurrent (4 Workers) | Concurrent (8 Workers) | Speedup (4 Workers) | Speedup (8 Workers) |
|---|---:|---:|---:|---:|---:|---:|
| **Small Dataset** | 1,000 | 2,397 ms (~2.4 s) | 1,058 ms (~1.1 s) | 1,002 ms (~1.0 s) | **2.27x** | **2.39x** |
| **Medium Dataset** | 10,000 | 90,240 ms (~90.2 s) | 80,090 ms (~80.1 s) | 69,759 ms (~69.8 s) | **1.13x** | **1.29x** |
| **Duplicate-Heavy** | 10,000 | 84,393 ms (~84.4 s) | 63,004 ms (~63.0 s) | 65,936 ms (~65.9 s) | **1.34x** | **1.28x** |

---

## Metric Formulas

$$\text{Throughput (MB/s)} = \frac{\text{Total Processed Input MB}}{\text{Elapsed Time (Seconds)}}$$

$$\text{Deduplication Ratio (\%)} = \left(\frac{\text{Duplicate Files}}{\text{Total Files}}\right) \times 100$$

$$\text{Storage Saved (\%)} = \left(1 - \frac{\text{Total Stored Archive Bytes}}{\text{Total Original Input Bytes}}\right) \times 100$$

$$\text{Speedup Factor} = \frac{\text{Sequential Execution Time (1 Worker)}}{\text{Concurrent Execution Time (N Workers)}}$$

---

## How to Reproduce Locally

```bash
# Compile test classes and build dependency classpath
mvn test-compile
mvn dependency:build-classpath -Dmdep.outputFile=target/cp.txt

# Run the benchmark runner
CP=$(cat target/cp.txt):target/classes:target/test-classes
java -cp "$CP" benchmark.BackupBenchmarkRunner
```

The raw machine-readable JSON results will be automatically written to [`results/latest.json`](results/latest.json).

