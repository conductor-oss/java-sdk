# Data Compression

A log aggregation service stores terabytes of raw event data. Before archiving, each dataset needs analysis to pick the best compression algorithm, actual compression, and verification that the compressed output decompresses to an identical byte stream. Choosing gzip for highly repetitive data wastes 40% of potential savings versus LZ4.

## Pipeline

```
[cmp_analyze_data]
     |
     v
[cmp_choose_algorithm]
     |
     v
[cmp_compress_data]
     |
     v
[cmp_verify_integrity]
     |
     v
[cmp_report_savings]
```

**Workflow inputs:** `records`, `targetFormat`

## Workers

**AnalyzeDataWorker** (task: `cmp_analyze_data`)

Analyzes data to determine size and record count.

- Reads `records`. Writes `records`, `recordCount`, `sizeBytes`

**ChooseAlgorithmWorker** (task: `cmp_choose_algorithm`)

Chooses a compression algorithm based on data size.

- Rounds with `math.round()`
- Reads `originalSizeBytes`. Writes `algorithm`, `estimatedRatio`

**CompressDataWorker** (task: `cmp_compress_data`)

Compresses data with the chosen algorithm.

- Truncates strings to first 16 character(s), generates uuids, rounds with `math.round()`, applies `math.floor()`
- Reads `originalSize`. Writes `compressedSize`, `checksum`, `algorithm`

**ReportSavingsWorker** (task: `cmp_report_savings`)

Reports compression savings.

- Formats output strings
- Reads `originalSize`, `compressedSize`, `verified`. Writes `compressionRatio`, `bytesSaved`, `summary`

**VerifyIntegrityWorker** (task: `cmp_verify_integrity`)

Verifies the integrity of compressed data.

- Reads `checksum`, `compressedSize`. Writes `integrityOk`, `checksum`

---

**30 tests** | Workflow: `data_compression` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
