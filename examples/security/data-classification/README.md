# Implementing Data Classification in Java with Conductor :  Data Store Scanning, PII Detection, Sensitivity Classification, and Label Application

A Java Conductor workflow example automating data classification. scanning data stores (databases, S3 buckets, file shares) for schema and column metadata, detecting personally identifiable information (PII) fields like emails, SSNs, and phone numbers, classifying each field by sensitivity level (public, internal, sensitive, restricted), and applying classification labels to the data catalog so downstream systems enforce the correct access controls and retention policies. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to know what sensitive data lives in your systems before you can protect it. Across dozens of databases, data lakes, and file stores, there are tables and columns containing Social Security numbers, email addresses, payment card numbers, health records, and other regulated data. often with column names like `field_7` or `user_data` that give no indication of what they contain. Regulations (GDPR, CCPA, HIPAA, PCI-DSS) require you to know where this data is, classify it by sensitivity, and apply appropriate protection. Without a complete inventory, you cannot enforce encryption, retention, or access control policies consistently.

Without orchestration, data classification is a manual, one-time project that is out of date before it finishes. A security analyst connects to each database, exports column metadata, eyeballs it for patterns that look like PII, adds notes to a spreadsheet, and then tries to get the data catalog team to apply labels. New tables appear daily and nobody rescans. When a privacy regulator asks "where do you store SSNs?", the answer is "we think we know, but we are not confident." Building this as a script means a timeout on one large database kills the entire scan, and there is no record of which stores were actually classified.

## The Solution

**You just write the PII detection rules and catalog labeling calls. Conductor handles the ordered scan-to-label pipeline, retries when database connections time out, and an audit trail proving which stores were scanned and what classifications were applied.**

Each classification step is a simple, independent worker. one scans data stores for schema and column metadata, one runs pattern matching and ML-based detection to identify PII fields, one assigns sensitivity levels (public, internal, sensitive, restricted) based on the detected data types and regulatory context, one applies the classification labels to the data catalog. Conductor takes care of executing them in strict order so no labels are applied without a complete PII detection pass, retrying if a database connection times out during scanning, and maintaining a complete audit trail that shows exactly which stores were scanned, what PII was detected, and what classifications were assigned,  essential for demonstrating compliance to regulators. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers form the classification pipeline: ScanDataStoresWorker extracts schema metadata, DetectPiiWorker identifies sensitive fields like SSNs and emails, ClassifyWorker assigns sensitivity levels based on regulatory context, and ApplyLabelsWorker writes classifications to the data catalog.

| Worker | Task | What It Does |
|---|---|---|
| **ScanDataStoresWorker** | `dc_scan_data_stores` | Connects to the target data store (database, S3 bucket, file share) and extracts schema metadata. table names, column names, data types, sample values,  for downstream analysis |
| **DetectPiiWorker** | `dc_detect_pii` | Runs pattern matching and heuristic analysis across the scanned columns to identify PII fields. email addresses, Social Security numbers, phone numbers, credit card numbers, health record identifiers |
| **ClassifyWorker** | `dc_classify` | Assigns a sensitivity level (public, internal, sensitive, restricted) to each field based on the detected PII type and applicable regulatory context (GDPR, CCPA, HIPAA, PCI-DSS) |
| **ApplyLabelsWorker** | `dc_apply_labels` | Writes the classification labels to the data catalog so downstream systems (access control, DLP, retention) can enforce policies based on the assigned sensitivity levels |

Workers implement security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations. the workflow logic stays the same.

### The Workflow

```
dc_scan_data_stores
    │
    ▼
dc_detect_pii
    │
    ▼
dc_classify
    │
    ▼
dc_apply_labels

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
java -jar target/data-classification-1.0.0.jar

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
java -jar target/data-classification-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_classification \
  --version 1 \
  --input '{}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_classification -s COMPLETED -c 5

```

## How to Extend

Each worker handles one classification step. connect ScanDataStoresWorker to your databases via JDBC or AWS Glue, DetectPiiWorker to Amazon Macie or Google Cloud DLP, and the scan-detect-classify-label workflow stays the same.

- **ScanDataStoresWorker** (`dc_scan_data_stores`): connect to real data stores using JDBC metadata queries, AWS Glue crawlers, or the Snowflake INFORMATION_SCHEMA to extract table and column metadata at scale
- **DetectPiiWorker** (`dc_detect_pii`): integrate with Amazon Macie, Google Cloud DLP, or Microsoft Purview for ML-powered PII detection, or use regex-based pattern libraries for known formats (SSN, email, credit card)
- **ClassifyWorker** (`dc_classify`): apply your organization's data classification policy, mapping detected PII types to sensitivity tiers based on regulatory requirements and business context
- **ApplyLabelsWorker** (`dc_apply_labels`): write labels to your data catalog (Apache Atlas, Collibra, Alation, AWS Glue Data Catalog) and trigger downstream policy enforcement (column-level encryption, row-level security, retention rules)

Integrate with your real databases and DLP tools, and the scan-detect-classify-label orchestration adapts without touching the workflow.

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
data-classification/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dataclassification/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataClassificationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyLabelsWorker.java
│       ├── ClassifyWorker.java
│       ├── DetectPiiWorker.java
│       └── ScanDataStoresWorker.java

```
