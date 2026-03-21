# Data Catalog in Java Using Conductor :  Asset Discovery, Classification, Tagging, and Search Indexing

A Java Conductor workflow example for building a data catalog. discovering data assets across schemas and data sources, classifying them by content type and PII sensitivity, applying metadata tags, and indexing everything for searchable catalog lookups. Uses [Conductor](https://github.

## The Problem

Your organization has data scattered across databases, data lakes, APIs, and file stores. Nobody knows what data exists, where it lives, whether it contains PII, or who owns it. You need to build a catalog that answers these questions. That means crawling data sources to discover tables, columns, and datasets at a configurable scan depth, classifying each asset by category and PII sensitivity (using rule-based patterns for emails, SSNs, phone numbers), tagging assets with metadata labels (owner, domain, freshness, quality tier), and indexing everything into a searchable catalog so analysts and engineers can find what they need.

Without orchestration, you'd write a monolithic crawler that connects to every data source, scans schemas inline, runs classification rules, generates tags, and writes to a search index in one pass. If the scan of a large database times out, the entire catalog build fails. If the process crashes after discovering 500 assets but before classifying them, you'd restart the full crawl. Adding a new data source type or classification rule means modifying deeply coupled code with no visibility into which step failed or how long each phase took.

## The Solution

**You just write the asset discovery, classification, metadata tagging, and catalog indexing workers. Conductor handles the discovery-to-index pipeline sequencing, retries when data source connections time out, and tracking of how many assets were discovered, classified, and indexed.**

Each stage of catalog building is a simple, independent worker. The discovery worker crawls the configured data source at the requested scan depth and returns a list of assets (tables, columns, datasets). The classifier applies rules to determine each asset's category and PII sensitivity level. The tagger generates metadata labels based on classification results (data domain, owner, sensitivity tier). The indexer writes the fully tagged assets into a searchable catalog index. Conductor executes them in sequence, passes the evolving asset list between steps, retries if a data source connection times out, and tracks exactly how many assets were discovered, classified, tagged, and indexed. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers build the catalog end-to-end: discovering data assets across schemas, classifying them by PII sensitivity and content type, tagging with metadata labels, and indexing into a searchable catalog.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyDataWorker** | `cg_classify_data` | Classifies data assets by PII and category. |
| **DiscoverAssetsWorker** | `cg_discover_assets` | Discovers data assets across schemas. |
| **IndexCatalogWorker** | `cg_index_catalog` | Indexes tagged assets into the catalog. |
| **TagMetadataWorker** | `cg_tag_metadata` | Tags data assets with metadata labels. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
cg_discover_assets
    │
    ▼
cg_classify_data
    │
    ▼
cg_tag_metadata
    │
    ▼
cg_index_catalog

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
java -jar target/data-catalog-1.0.0.jar

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
java -jar target/data-catalog-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_catalog \
  --version 1 \
  --input '{"dataSource": "api", "scanDepth": "sample-scanDepth", "classificationRules": "sample-classificationRules"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_catalog -s COMPLETED -c 5

```

## How to Extend

Connect the discovery worker to real data sources via AWS Glue crawlers or PostgreSQL information_schema, integrate ML-based classifiers for PII detection, and index into Elasticsearch, the catalog workflow runs unchanged.

- **DiscoverAssetsWorker** → connect to real data sources (PostgreSQL `information_schema`, AWS Glue crawlers, Snowflake `SHOW TABLES`, S3 bucket listings) to discover assets
- **ClassifyDataWorker** → integrate ML-based classifiers (Google DLP, AWS Macie) or custom regex rules to detect PII patterns, data quality issues, and content categories
- **TagMetadataWorker** → write tags to a real metadata store (Apache Atlas, DataHub, Amundsen, or a custom metadata database)
- **IndexCatalogWorker** → index tagged assets into Elasticsearch, OpenSearch, or Algolia for full-text search across your catalog

Connecting a real data source crawler or ML-based classifier requires no workflow changes, the discover-classify-tag-index pipeline remains the same as long as asset structures are preserved.

**Add new stages** by inserting tasks in `workflow.json`, for example, a lineage tracker that maps upstream/downstream dependencies, a freshness checker that flags stale datasets, or a data quality profiler that computes null rates and cardinality per column.

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
data-catalog/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/datacatalog/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataCatalogExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClassifyDataWorker.java
│       ├── DiscoverAssetsWorker.java
│       ├── IndexCatalogWorker.java
│       └── TagMetadataWorker.java
└── src/test/java/datacatalog/workers/
    ├── ClassifyDataWorkerTest.java        # 8 tests
    ├── DiscoverAssetsWorkerTest.java        # 8 tests
    ├── IndexCatalogWorkerTest.java        # 8 tests
    └── TagMetadataWorkerTest.java        # 8 tests

```
