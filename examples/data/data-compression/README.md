# Data Compression in Java Using Conductor : Algorithm Selection, Compression, Integrity Verification, and Savings Report

## The Problem

You need to compress datasets before archival, transfer, or storage; but not all data compresses the same way. Text-heavy records compress well with gzip, columnar data benefits from Snappy or LZ4, and already-compressed media gains little from further compression. You need to analyze the data to understand its size and structure, choose the right algorithm (gzip, LZ4, Snappy, zstd) based on the data profile, compress the data, verify that the compressed output is intact and decompressible, and report the actual compression ratio and bytes saved. If you compress corrupted data or skip verification, you might discover the archive is unrecoverable months later.

Without orchestration, you'd hardcode a single compression algorithm, skip the analysis step, and hope the data compresses well. If the compression fails halfway through a large dataset, you'd restart from scratch. There's no record of which algorithm was selected, what ratio was achieved, or whether integrity was verified. Just a compressed file and a prayer.

## The Solution

**You just write the data analysis, algorithm selection, compression, integrity verification, and savings reporting workers. Conductor handles the analyze-select-compress-verify-report sequence, retries when compression fails on large datasets, and crash recovery that resumes from the exact step that failed.**

Each stage of the compression pipeline is a simple, independent worker. The analyzer profiles the input data to measure size, record count, and data characteristics. The algorithm selector chooses the best compression method based on the analysis results and the target format. The compressor applies the selected algorithm and computes a checksum. The verifier confirms the compressed output's integrity by validating the checksum and record count. The reporter calculates the compression ratio and storage savings. Conductor executes them in sequence, passes analysis results to the algorithm selector and compression output to the verifier, retries if compression fails on a large dataset, and resumes from the exact step where it left off.

### What You Write: Workers

Five workers cover the intelligent compression pipeline: profiling data characteristics, selecting the optimal algorithm, compressing the data, verifying integrity via checksums, and reporting the compression ratio and savings.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeDataWorker** | `cmp_analyze_data` | Analyzes data to determine size and record count. |
| **ChooseAlgorithmWorker** | `cmp_choose_algorithm` | Chooses a compression algorithm based on data size. |
| **CompressDataWorker** | `cmp_compress_data` | Simulates compressing data with the chosen algorithm. |
| **ReportSavingsWorker** | `cmp_report_savings` | Reports compression savings. |
| **VerifyIntegrityWorker** | `cmp_verify_integrity` | Verifies the integrity of compressed data. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
cmp_analyze_data
 │
 ▼
cmp_choose_algorithm
 │
 ▼
cmp_compress_data
 │
 ▼
cmp_verify_integrity
 │
 ▼
cmp_report_savings

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
