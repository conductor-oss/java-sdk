# Data Anonymization in Java Using Conductor :  PII Detection, Generalization, Suppression, and k-Anonymity Verification

A Java Conductor workflow example for data anonymization. scanning datasets for personally identifiable information, generalizing quasi-identifiers (age ranges, zip code prefixes), suppressing direct identifiers (names, SSNs, emails), and verifying that the result meets k-anonymity thresholds. Uses [Conductor](https://github.## The Problem

You need to share or analyze datasets that contain personal information. Patient records for research, customer data for analytics, employee records for benchmarking. Regulations like GDPR, HIPAA, and CCPA require you to anonymize this data before it leaves controlled environments. That means scanning every field to identify PII (names, emails, SSNs, phone numbers, addresses), generalizing quasi-identifiers so individuals can't be re-identified (replacing exact ages with ranges, truncating zip codes), suppressing direct identifiers entirely (replacing names and SSNs with `[REDACTED]`), and verifying the result meets a k-anonymity threshold so no individual can be singled out from the anonymized dataset.

Without orchestration, you'd write a single anonymization script that scans fields, applies transformations inline, and hopes everything was covered. If the PII scanner misclassifies a field, downstream suppressions silently fail. There's no audit trail showing which fields were identified, which were generalized vs: suppressed, and whether the final dataset actually meets the required anonymization level. Adding a new anonymization technique (differential privacy, pseudonymization) means rewriting tightly coupled code.

## The Solution

**You just write the PII detection, generalization, suppression, and verification workers. Conductor handles strict sequencing from PII detection through verification, audit-grade tracking of every transformation decision, and retries when external classification services are unavailable.**

Each stage of the anonymization pipeline is a simple, independent worker. The PII identifier scans the dataset and classifies fields as direct identifiers, quasi-identifiers, or safe. The generalizer applies range-based transformations to quasi-identifiers (ages become ranges, zip codes become prefixes). The suppressor replaces direct identifiers with `[REDACTED]` at the configured anonymization level. The verifier checks that no PII leaks through and computes the k-anonymity score of the output dataset. Conductor executes them in sequence, passes the evolving dataset between steps, retries if a scan fails, and tracks every transformation decision with full audit visibility. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers implement the anonymization pipeline: scanning for PII fields, generalizing quasi-identifiers like age and zip code, suppressing direct identifiers with redaction, and verifying k-anonymity compliance.

| Worker | Task | What It Does |
|---|---|---|
| **GeneralizeDataWorker** | `an_generalize_data` | Generalizes quasi-identifier and selected direct-identifier fields. |
| **IdentifyPiiWorker** | `an_identify_pii` | Identifies PII fields in the dataset. |
| **SuppressFieldsWorker** | `an_suppress_fields` | Suppresses direct-identifier fields by replacing values with [REDACTED]. |
| **VerifyAnonymizationWorker** | `an_verify_anonymization` | Verifies that anonymization was applied correctly. |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

### The Workflow

```
an_identify_pii
    │
    ▼
an_generalize_data
    │
    ▼
an_suppress_fields
    │
    ▼
an_verify_anonymization
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
java -jar target/data-anonymization-1.0.0.jar
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
java -jar target/data-anonymization-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_anonymization \
  --version 1 \
  --input '{"dataset": "test-value", "anonymizationLevel": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_anonymization -s COMPLETED -c 5
```

## How to Extend

Integrate AWS Macie or Google DLP for PII detection, implement real generalization hierarchies and suppression strategies, and the GDPR-compliant anonymization workflow runs unchanged.

- **IdentifyPiiWorker** → integrate a real PII detection engine (AWS Macie, Google DLP API, Microsoft Presidio) for pattern-based and ML-based field classification
- **GeneralizeDataWorker** → implement configurable generalization hierarchies (city to state to country, exact age to 5-year range to decade)
- **SuppressFieldsWorker** → support multiple suppression strategies beyond `[REDACTED]`. Hashing, tokenization, or format-preserving encryption for fields that need referential integrity
- **VerifyAnonymizationWorker** → compute real k-anonymity, l-diversity, or t-closeness metrics; fail the workflow if the anonymized dataset doesn't meet the required privacy threshold

Integrating a real PII scanner or adjusting generalization hierarchies leaves the workflow intact, provided each worker outputs the expected anonymized dataset structure.

**Add new stages** by inserting tasks in `workflow.json`, for example, a consent-check step that filters records based on opt-in status, a pseudonymization step that generates consistent fake identifiers, or an audit-log writer that records every anonymization decision for compliance reporting.

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
data-anonymization/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dataanonymization/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataAnonymizationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GeneralizeDataWorker.java
│       ├── IdentifyPiiWorker.java
│       ├── SuppressFieldsWorker.java
│       └── VerifyAnonymizationWorker.java
└── src/test/java/dataanonymization/workers/
    ├── GeneralizeDataWorkerTest.java        # 5 tests
    ├── IdentifyPiiWorkerTest.java        # 4 tests
    ├── SuppressFieldsWorkerTest.java        # 3 tests
    └── VerifyAnonymizationWorkerTest.java        # 4 tests
```
