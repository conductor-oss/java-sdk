# Public Records in Java with Conductor

Fulfills a public records request (FOIA): receiving the request, searching government databases, verifying document authenticity, redacting sensitive information, and releasing records to the requester. Uses [Conductor](https://github.## The Problem

You need to fulfill a public records request (FOIA, state open records law). A requester submits a request for specific records, the records are searched across government databases and archives, found records are verified for authenticity, sensitive information is redacted (SSNs, law enforcement details, attorney-client privilege), and the redacted records are released to the requester. Releasing un-redacted records exposes private information and violates privacy laws; denying without proper search violates open records laws.

Without orchestration, you'd manage records requests through email and shared drives .  manually searching across multiple databases, tracking which documents need redaction, coordinating with legal counsel on exemptions, and meeting statutory response deadlines that vary by jurisdiction.

## The Solution

**You just write the request intake, database search, document verification, redaction, and record release logic. Conductor handles search retries, redaction sequencing, and records request audit trails.**

Each records request concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (request, search, verify, redact, release), tracking every request with timestamps for statutory deadline compliance, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Request validation, records search, redaction, and delivery workers handle FOIA and public records requests through auditable stages.

| Worker | Task | What It Does |
|---|---|---|
| **RedactWorker** | `pbr_redact` | Redacts PII and sensitive information from the found documents before release |
| **ReleaseWorker** | `pbr_release` | Releases the redacted public records documents to the requester |
| **RequestWorker** | `pbr_request` | Receives the FOIA request from the requester and assigns a tracking ID |
| **SearchWorker** | `pbr_search` | Searches government databases and archives for documents matching the request query |
| **VerifyWorker** | `pbr_verify` | Verifies document authenticity and checks for applicable exemptions before release |

Workers simulate government operations .  application processing, compliance checks, notifications ,  with realistic outputs. Replace with real agency system integrations and the workflow stays the same.

### The Workflow

```
pbr_request
    │
    ▼
pbr_search
    │
    ▼
pbr_verify
    │
    ▼
pbr_redact
    │
    ▼
pbr_release
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
java -jar target/public-records-1.0.0.jar
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
java -jar target/public-records-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow pbr_public_records \
  --version 1 \
  --input '{"requesterId": "TEST-001", "recordType": "test-value", "query": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w pbr_public_records -s COMPLETED -c 5
```

## How to Extend

Swap each worker for your real FOIA systems .  your records management platform for document search, your redaction tool for sensitive content removal, your citizen portal for record release, and the workflow runs identically in production.

- **Request handler**: accept requests via your public records portal with requester identification and scope clarification
- **Records searcher**: query across government databases, document management systems (Laserfiche, OnBase), and email archives
- **Record verifier**: verify document authenticity, check for applicable exemptions (FOIA exemptions 1-9, state-specific exemptions), and flag for legal review
- **Redactor**: apply automated redaction (PII detection via AWS Comprehend, custom regex patterns) with human review for sensitive exemptions
- **Record releaser**: compile the response package, generate a Vaughn index for withheld documents, and deliver to the requester within statutory deadlines

Switch redaction tools or search backends and the records pipeline operates identically.

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
public-records-public-records/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/publicrecords/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PublicRecordsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── RedactWorker.java
│       ├── ReleaseWorker.java
│       ├── RequestWorker.java
│       ├── SearchWorker.java
│       └── VerifyWorker.java
└── src/test/java/publicrecords/workers/
    ├── RedactWorkerTest.java
    ├── ReleaseWorkerTest.java
    ├── RequestWorkerTest.java
    └── SearchWorkerTest.java
```
