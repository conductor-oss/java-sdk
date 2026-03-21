# Data Export in Java Using Conductor :  Parallel Multi-Format Export (CSV, JSON, Excel) and Bundling

A Java Conductor workflow example for data export: querying a data source, then exporting the results to CSV, JSON, and Excel simultaneously using `FORK_JOIN` parallelism, and bundling all exported files into a single archive for download or delivery. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

Users and downstream systems need data in different formats. The finance team wants Excel with formatted columns. The integration partner needs JSON. The data analyst wants CSV for import into R or pandas. You need to export the same dataset to all three formats, do it as fast as possible (in parallel, not sequentially), and bundle the results into a single deliverable. If the Excel export fails due to a formatting issue, the CSV and JSON exports should still complete successfully.

Without orchestration, you'd write a single export method that generates CSV, then JSON, then Excel sequentially. Tripling the total export time. You'd manage thread pools manually to parallelize, add try/catch blocks around each format so one failure doesn't crash the others, and build your own logic to wait for all exports to finish before bundling. Adding a new export format (Parquet, XML, PDF) means modifying the parallelism and join logic every time.

## The Solution

**You just write the data preparation, CSV/JSON/Excel export, and bundle workers. Conductor handles parallel format generation via FORK_JOIN, independent retries per format, and automatic join-then-bundle sequencing.**

Each export format is a simple, independent worker. The data preparation worker queries the source and structures the data with headers. The CSV, JSON, and Excel workers each receive the same prepared data and produce their format-specific output. Conductor's `FORK_JOIN` runs all three exports simultaneously, waits for all to complete, and then the bundler packages them into a single archive. If one format fails, Conductor retries just that export. If the process crashes after two of three exports finish, Conductor resumes only the incomplete one. You get all of that, without writing a single line of parallelism or join logic.

### What You Write: Workers

Five workers handle the parallel export pipeline: preparing the data from a source query, generating CSV, JSON, and Excel formats simultaneously via FORK_JOIN, and bundling all formats into a single downloadable archive.

| Worker | Task | What It Does |
|---|---|---|
| **BundleExportsWorker** | `dx_bundle_exports` | Bundles all exported files into a single archive and uploads to destination. |
| **ExportCsvWorker** | `dx_export_csv` | Exports data to CSV format. |
| **ExportExcelWorker** | `dx_export_excel` | Exports data to Excel format. |
| **ExportJsonWorker** | `dx_export_json` | Exports data to JSON format. |
| **PrepareDataWorker** | `dx_prepare_data` | Prepares data for export by querying the data source. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
dx_prepare_data
    │
    ▼
FORK_JOIN
    ├── dx_export_csv
    ├── dx_export_json
    └── dx_export_excel
    │
    ▼
JOIN (wait for all branches)
dx_bundle_exports

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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/data-export-1.0.0.jar

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
java -jar target/data-export-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_export \
  --version 1 \
  --input '{"query": "What is workflow orchestration?", "formats": "json", "destination": "production"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_export -s COMPLETED -c 5

```

## How to Extend

Replace the format workers with Apache Commons CSV, Jackson JSON, and Apache POI for Excel generation, then upload the bundle to S3, the parallel export workflow runs unchanged.

- **PrepareDataWorker** → query a real database, API, or data warehouse to fetch records based on the input query parameters
- **ExportCsvWorker** → use Apache Commons CSV or OpenCSV to write real CSV files with proper quoting and encoding
- **ExportJsonWorker** → serialize to JSON with Jackson or Gson, supporting pretty-print and streaming for large datasets
- **ExportExcelWorker** → generate real Excel files with Apache POI, including formatted headers, column widths, and data types
- **BundleExportsWorker** → create a ZIP archive of all exports and upload to S3, send via email, or store for download

Replacing any format worker with a production library like Apache POI or Jackson leaves the parallel export workflow unchanged, as long as each returns a file reference and metadata.

**Add new export formats** by adding a new branch to the `FORK_JOIN` in `workflow.json`, for example, Parquet for data engineering teams, PDF reports for executives, or XML for legacy system integrations. Each new format is just another parallel worker.

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
data-export/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dataexport/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataExportExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── BundleExportsWorker.java
│       ├── ExportCsvWorker.java
│       ├── ExportExcelWorker.java
│       ├── ExportJsonWorker.java
│       └── PrepareDataWorker.java
└── src/test/java/dataexport/workers/
    ├── BundleExportsWorkerTest.java        # 6 tests
    ├── ExportCsvWorkerTest.java        # 5 tests
    ├── ExportExcelWorkerTest.java        # 4 tests
    ├── ExportJsonWorkerTest.java        # 4 tests
    └── PrepareDataWorkerTest.java        # 6 tests

```
