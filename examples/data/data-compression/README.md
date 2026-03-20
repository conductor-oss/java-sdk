# Data Compression in Java Using Conductor :  Algorithm Selection, Compression, Integrity Verification, and Savings Report

A Java Conductor workflow example for intelligent data compression. analyzing data characteristics to determine size and structure, selecting the optimal compression algorithm based on data profile and target format, compressing the data, verifying integrity via checksums, and reporting the compression ratio and storage savings. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to compress datasets before archival, transfer, or storage; but not all data compresses the same way. Text-heavy records compress well with gzip, columnar data benefits from Snappy or LZ4, and already-compressed media gains little from further compression. You need to analyze the data to understand its size and structure, choose the right algorithm (gzip, LZ4, Snappy, zstd) based on the data profile, compress the data, verify that the compressed output is intact and decompressible, and report the actual compression ratio and bytes saved. If you compress corrupted data or skip verification, you might discover the archive is unrecoverable months later.

Without orchestration, you'd hardcode a single compression algorithm, skip the analysis step, and hope the data compresses well. If the compression fails halfway through a large dataset, you'd restart from scratch. There's no record of which algorithm was selected, what ratio was achieved, or whether integrity was verified. Just a compressed file and a prayer.

## The Solution

**You just write the data analysis, algorithm selection, compression, integrity verification, and savings reporting workers. Conductor handles the analyze-select-compress-verify-report sequence, retries when compression fails on large datasets, and crash recovery that resumes from the exact step that failed.**

Each stage of the compression pipeline is a simple, independent worker. The analyzer profiles the input data to measure size, record count, and data characteristics. The algorithm selector chooses the best compression method based on the analysis results and the target format. The compressor applies the selected algorithm and computes a checksum. The verifier confirms the compressed output's integrity by validating the checksum and record count. The reporter calculates the compression ratio and storage savings. Conductor executes them in sequence, passes analysis results to the algorithm selector and compression output to the verifier, retries if compression fails on a large dataset, and resumes from the exact step where it left off. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Five workers cover the intelligent compression pipeline: profiling data characteristics, selecting the optimal algorithm, compressing the data, verifying integrity via checksums, and reporting the compression ratio and savings.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeDataWorker** | `cmp_analyze_data` | Analyzes data to determine size and record count. |
| **ChooseAlgorithmWorker** | `cmp_choose_algorithm` | Chooses a compression algorithm based on data size. |
| **CompressDataWorker** | `cmp_compress_data` | Simulates compressing data with the chosen algorithm. |
| **ReportSavingsWorker** | `cmp_report_savings` | Reports compression savings. |
| **VerifyIntegrityWorker** | `cmp_verify_integrity` | Verifies the integrity of compressed data. |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

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

## Example Output

```
=== Data Compression Workflow Demo ===

Step 1: Registering task definitions...
  Registered: cmp_analyze_data, cmp_choose_algorithm, cmp_compress_data, cmp_verify_integrity, cmp_report_savings

Step 2: Registering workflow 'data_compression'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [analyze]
  [algorithm] Selected \"" + algorithm + "\" for
  [compress]
  [report]
  [verify] Integrity check: checksum=

  Status: COMPLETED
  Output: {records=..., recordCount=..., sizeBytes=..., algorithm=...}

Result: PASSED
```

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build
```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build
```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/data-compression-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/data-compression-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_compression \
  --version 1 \
  --input '{"records": ["item-1", "item-2", "item-3"]}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_compression -s COMPLETED -c 5
```

## How to Extend

Replace the simulated compressor with real zstd, LZ4, or Snappy via Apache Commons Compress, add SHA-256 checksum verification, and the compression pipeline runs unchanged.

- **AnalyzeDataWorker** → profile real data by sampling records to determine entropy, data type distribution, and repetitiveness for smarter algorithm selection
- **ChooseAlgorithmWorker** → implement a real selection matrix: zstd for general-purpose high compression, LZ4 for speed-critical pipelines, Snappy for Hadoop/Spark compatibility, gzip for maximum portability
- **CompressDataWorker** → use `java.util.zip`, Apache Commons Compress, or Aircompressor to apply real compression with configurable levels
- **VerifyIntegrityWorker** → decompress a sample block and compare checksums (CRC32, SHA-256) to confirm the archive is recoverable
- **ReportSavingsWorker** → write compression metrics to a monitoring system (Datadog, CloudWatch) or append to a cost-tracking spreadsheet

Swapping in a real compression library or a different algorithm selection strategy does not affect the workflow, provided each worker outputs the expected size and checksum fields.

**Add new stages** by inserting tasks in `workflow.json`, for example, an encryption step after compression, a split step that chunks large archives into multipart uploads, or a deduplication pass before compression to maximize savings.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>
```

## Project Structure

```
data-compression/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/datacompression/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataCompressionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeDataWorker.java
│       ├── ChooseAlgorithmWorker.java
│       ├── CompressDataWorker.java
│       ├── ReportSavingsWorker.java
│       └── VerifyIntegrityWorker.java
└── src/test/java/datacompression/workers/
    ├── AnalyzeDataWorkerTest.java        # 6 tests
    ├── ChooseAlgorithmWorkerTest.java        # 6 tests
    ├── CompressDataWorkerTest.java        # 6 tests
    ├── ReportSavingsWorkerTest.java        # 6 tests
    └── VerifyIntegrityWorkerTest.java        # 6 tests
```
