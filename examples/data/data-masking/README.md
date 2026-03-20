# Data Masking in Java Using Conductor :  PII Detection, SSN Masking, Email/Phone Obfuscation

A Java Conductor workflow example for data masking: loading records, detecting PII fields (SSNs, emails, phone numbers), applying format-preserving masks (SSN `123-45-6789` becomes `***-**-6789`, email `alice@example.com` becomes `a***@example.com`, phone `555-123-4567` becomes `***-***-4567`), and emitting the masked dataset with a summary of what was protected. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to share production data with developers, testers, or analytics teams; but it contains Social Security numbers, email addresses, and phone numbers that must never leave the production boundary unprotected. Unlike anonymization (which destroys data utility), masking preserves the format and partial values so the data remains useful for testing and analysis. SSNs need to show only the last four digits. Emails need to hide the username while preserving the domain. Phone numbers need the digits replaced while keeping the format. Each PII type requires a different masking strategy, and you need to track exactly how many fields were detected and masked.

Without orchestration, you'd write a single script that scans fields with regex, applies masks inline, and outputs the result. If the SSN masking logic has a bug that leaks full SSNs, there's no separation between detection and masking steps to catch it. If the process crashes after masking SSNs but before masking emails, you'd restart from scratch. There's no audit record showing how many PII fields were found and which masking policy was applied.

## The Solution

**You just write the PII detection, SSN masking, email/phone obfuscation, and emission workers. Conductor handles the detect-then-mask sequencing, a complete audit trail of how many PII fields were found and protected, and retries if any masking step fails.**

Each masking concern is a simple, independent worker. The PII detector scans records to identify which fields contain SSNs, emails, and phone numbers based on the configured masking policy. The SSN masker replaces all but the last four digits with asterisks. The email/phone masker obfuscates usernames and digits while preserving format and domains. The emitter produces the final masked dataset with counts of how many fields were detected and masked per type. Conductor executes them in sequence, passes progressively masked records between steps, retries if a step fails, and provides a complete audit trail showing exactly what was detected and masked. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Five workers implement format-preserving masking: loading records, detecting PII fields (SSNs, emails, phone numbers), masking SSNs to show only last four digits, obfuscating emails and phones while preserving format, and emitting the masked dataset with protection counts.

| Worker | Task | What It Does |
|---|---|---|
| **DetectPiiWorker** | `mk_detect_pii` | Detects PII fields (SSN, email, phone) in records. |
| **EmitMaskedWorker** | `mk_emit_masked` | Emits the final masked records and produces a summary. |
| **LoadRecordsWorker** | `mk_load_records` | Loads records for PII masking. |
| **MaskEmailPhoneWorker** | `mk_mask_email_phone` | Mask Email Phone. Computes and returns records, masked count |
| **MaskSsnWorker** | `mk_mask_ssn` | Masks SSN fields in records (e.g. "123-45-6789" -> "***-**-6789"). |

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
mk_load_records
    │
    ▼
mk_detect_pii
    │
    ▼
mk_mask_ssn
    │
    ▼
mk_mask_email_phone
    │
    ▼
mk_emit_masked
```

## Example Output

```
=== Data Masking Workflow Demo ===

Step 1: Registering task definitions...
  Registered: mk_load_records, mk_detect_pii, mk_mask_ssn, mk_mask_email_phone, mk_emit_masked

Step 2: Registering workflow 'data_masking'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [detect] Detected
  [emit]
  [load] Loaded
  [mask-contact] Masked
  [mask-ssn] Masked

  Status: COMPLETED
  Output: {records=..., piiFields=..., piiFieldCount=..., summary=...}

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
java -jar target/data-masking-1.0.0.jar
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
java -jar target/data-masking-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_masking \
  --version 1 \
  --input '{"records": "sample-records", "name": "sample-name", "Alice Smith": "sample-Alice Smith", "ssn": "sample-ssn", "123-45-6789": "sample-123-45-6789", "email": "user@example.com", "alice@example.com": "sample-alice@example.com", "phone": "sample-phone", "Bob Jones": "sample-Bob Jones", "987-65-4321": "sample-987-65-4321", "bob@company.com": "sample-bob@company.com", "Charlie Brown": "sample-Charlie Brown", "456-78-9012": "sample-456-78-9012", "charlie@test.org": "sample-charlie@test.org"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_masking -s COMPLETED -c 5
```

## How to Extend

Integrate Google DLP or Microsoft Presidio for PII detection, implement deterministic masking for referential integrity, and the format-preserving masking workflow runs unchanged.

- **LoadRecordsWorker** → read records from a production database snapshot, S3 export, or change data capture stream
- **DetectPiiWorker** → integrate Google DLP, AWS Macie, or Microsoft Presidio for ML-based PII detection beyond regex patterns
- **MaskSsnWorker** → implement deterministic masking (same input always produces same output) for referential integrity across tables, or use format-preserving encryption
- **MaskEmailPhoneWorker** → add configurable masking levels (full redaction vs, partial masking vs. domain-only) based on the consumer's access tier
- **EmitMaskedWorker** → write masked records to a staging database, publish to a sanitized data catalog, or stream to a developer-facing replica

Switching to deterministic masking or integrating a real PII detection engine leaves the workflow intact, as long as each worker returns records with the expected masked field formats.

**Add new masking types** by inserting tasks in `workflow.json`, for example, credit card masking (show last four digits), address masking (generalize to zip code only), or name masking with consistent fake names using Faker for realistic test data.

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
data-masking/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/datamasking/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataMaskingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DetectPiiWorker.java
│       ├── EmitMaskedWorker.java
│       ├── LoadRecordsWorker.java
│       ├── MaskEmailPhoneWorker.java
│       └── MaskSsnWorker.java
└── src/test/java/datamasking/workers/
    ├── DetectPiiWorkerTest.java        # 4 tests
    ├── EmitMaskedWorkerTest.java        # 3 tests
    ├── LoadRecordsWorkerTest.java        # 4 tests
    ├── MaskEmailPhoneWorkerTest.java        # 5 tests
    └── MaskSsnWorkerTest.java        # 4 tests
```
