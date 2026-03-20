# Implementing Data Masking in Java with Conductor :  Field Identification, Strategy Selection, Masking Application, and Output Validation

A Java Conductor workflow example automating data masking .  scanning a data source to identify sensitive fields (SSNs, emails, credit card numbers), selecting the appropriate masking strategy (tokenization, redaction, format-preserving encryption, pseudonymization) based on the data's intended purpose, applying the masks to every sensitive field, and validating that no PII remains in the output while referential integrity is preserved. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to share production-like data with development teams, analytics pipelines, or third-party vendors; but the data contains Social Security numbers, email addresses, credit card numbers, health records, and other regulated PII that cannot be exposed. Each field type requires a different masking approach: SSNs should be redacted or tokenized, emails need format-preserving pseudonymization so downstream systems still process them correctly, credit card numbers must preserve the BIN prefix for payment testing. After masking, you must verify that no PII leaked through (partial masks, edge cases, NULL handling) and that referential integrity is maintained (the same customer ID maps to the same masked ID across all tables).

Without orchestration, data masking is a brittle script that somebody wrote once and nobody maintains. It hardcodes which columns to mask, uses the same blunt approach (replace everything with `***`) regardless of the field type, and has no validation step .  so developers discover that the masked data breaks their tests because foreign keys no longer match. When a new table with sensitive data is added, nobody updates the script. There is no audit trail showing what was masked, when, or by whom, making it impossible to prove compliance to regulators who require evidence that non-production environments contain only masked data.

## The Solution

**You just write the field detection and masking transformations. Conductor handles the strict field-identification-to-validation sequence, retries when data source connections drop, and a complete audit proving exactly which fields were masked, how, and whether validation passed.**

Each masking step is a simple, independent worker .  one scans the data source to identify all sensitive fields, one selects the right masking strategy (tokenization for IDs, redaction for SSNs, format-preserving encryption for emails) based on the data's purpose, one applies the masks across all identified fields, one validates that the output contains no residual PII and that referential integrity is preserved across related tables. Conductor takes care of executing them in strict order so no data is released without validation, retrying if the data source connection drops mid-scan, and maintaining a complete audit trail proving exactly which fields were identified, what strategy was applied, and whether the validation passed ,  essential for demonstrating compliance with GDPR, CCPA, and HIPAA data minimization requirements. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

The masking pipeline chains DmIdentifyFieldsWorker to discover sensitive fields, DmSelectStrategyWorker to choose the right technique (tokenization, redaction, or FPE), DmApplyMaskingWorker to transform the data, and DmValidateOutputWorker to verify no PII leaked and referential integrity is preserved.

| Worker | Task | What It Does |
|---|---|---|
| **DmIdentifyFieldsWorker** | `dm_identify_fields` | Scans the data source and identifies all sensitive fields .  email columns, SSN fields, credit card numbers, phone numbers, health record identifiers ,  returning the field list with detected PII types |
| **DmSelectStrategyWorker** | `dm_select_strategy` | Selects the masking strategy for each field based on the data's intended purpose .  tokenization for fields that need reversibility, redaction for SSNs, format-preserving encryption for emails used in testing |
| **DmApplyMaskingWorker** | `dm_apply_masking` | Applies the selected masking technique to every identified sensitive field .  tokenizing IDs, redacting SSNs, pseudonymizing email addresses ,  while preserving data format and referential integrity |
| **DmValidateOutputWorker** | `dm_validate_output` | Verifies that no PII remains in the masked output by re-scanning for sensitive patterns, checks that referential integrity is maintained across related tables, and confirms the masked data is usable for its intended purpose |

Workers simulate security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations .  the workflow logic stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
dm_identify_fields
    │
    ▼
dm_select_strategy
    │
    ▼
dm_apply_masking
    │
    ▼
dm_validate_output
```

## Example Output

```
=== Example 392: Data Masking ===

Step 1: Registering task definitions...
  Registered: dm_identify_fields, dm_select_strategy, dm_apply_masking, dm_validate_output

Step 2: Registering workflow 'data_masking_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [mask] 18 fields masked: emails tokenized, SSNs redacted
  [identify]
  [strategy] Masking strategy for
  [validate] No PII found in masked output, referential integrity maintained

  Status: COMPLETED
  Output: {apply_masking=..., processed=..., identify_fieldsId=..., success=...}

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
  --input '{}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_masking -s COMPLETED -c 5
```

## How to Extend

Each worker owns one masking phase .  connect DmIdentifyFieldsWorker to Amazon Macie or Microsoft Purview, DmApplyMaskingWorker to Delphix or Vault Transit for tokenization, and the identify-strategize-mask-validate workflow stays the same.

- **DmIdentifyFieldsWorker** (`dm_identify_fields`): integrate with Amazon Macie, Google Cloud DLP, or Microsoft Purview to detect sensitive fields, or use your data catalog labels from a prior classification workflow
- **DmSelectStrategyWorker** (`dm_select_strategy`): pull masking policies from a rules engine or configuration database that maps field types and use cases to masking techniques (tokenization via Vault Transit, FPE via Voltage, redaction, k-anonymity)
- **DmApplyMaskingWorker** (`dm_apply_masking`): execute the actual masking using Delphix, Informatica Dynamic Data Masking, or custom implementations with libraries like java-faker for pseudonymization and Bouncy Castle for format-preserving encryption
- **DmValidateOutputWorker** (`dm_validate_output`): re-scan the masked output with the same PII detection engine to verify zero residual exposure, run referential integrity checks across masked tables, and produce a validation certificate for compliance records

Plug in your production DLP engine and masking library, and the identify-mask-validate orchestration carries forward with no changes.

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
│       ├── DmApplyMaskingWorker.java
│       ├── DmIdentifyFieldsWorker.java
│       ├── DmSelectStrategyWorker.java
│       └── DmValidateOutputWorker.java
└── src/test/java/datamasking/workers/
    ├── DmApplyMaskingWorkerTest.java        # 8 tests
    ├── DmIdentifyFieldsWorkerTest.java        # 7 tests
    ├── DmSelectStrategyWorkerTest.java        # 8 tests
    └── DmValidateOutputWorkerTest.java        # 8 tests
```
