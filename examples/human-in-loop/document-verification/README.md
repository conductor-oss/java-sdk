# Document Verification in Java Using Conductor :  AI Data Extraction, Human Verification via WAIT, and Verified Data Storage

A Java Conductor workflow example for document verification .  using AI/OCR to extract structured data from a document (name, date, amount, ID numbers), pausing at a WAIT task for a human to verify and correct the extracted data against the original document, and then storing the human-verified data as the authoritative record. Demonstrates the AI-extracts-human-verifies pattern for intelligent document processing. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to process documents .  invoices, contracts, tax forms, identity documents ,  by extracting structured data from unstructured images or PDFs. AI/OCR models extract fields like names, dates, amounts, and document numbers, along with a confidence score for each extraction. But AI extraction is not perfect ,  handwriting misreads, low-quality scans, and unusual layouts cause errors. A human must verify the extracted data against the original document, correcting any mistakes before the data enters your system of record. Without verification, OCR errors propagate into your database ,  wrong amounts on invoices, misspelled names on contracts, incorrect tax IDs.

Without orchestration, you'd call the OCR API, store the raw extraction in a database, email a reviewer with a link, poll for their corrections, and then update the database with verified data. If the OCR API times out on a large batch, you'd need retry logic. If the system crashes after the reviewer corrects the data but before it is stored, the verified corrections are lost. There is no visibility into how many documents are awaiting verification, what the AI confidence scores are, or how often humans correct the AI's output.

## The Solution

**You just write the AI/OCR extraction and verified-data storage workers. Conductor handles the pause for human verification and the confidence-tracking pipeline.**

The WAIT task is the key pattern here. After the AI extracts data with confidence scores, the workflow pauses at the WAIT task. Conductor presents the extracted fields and confidence to the human verifier, who corrects any errors and submits the verified data via the API. The store worker then persists the human-verified data as the authoritative record. Conductor takes care of holding the extracted data while a reviewer verifies it, passing the reviewer's corrected data to storage, tracking extraction confidence versus human corrections (useful for retraining the AI model), and maintaining a complete audit trail from raw document through AI extraction to human-verified output. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

AiExtractWorker runs OCR to pull structured fields with confidence scores, and StoreVerifiedWorker persists the human-corrected data, the verification step between them pauses durably for the reviewer.

| Worker | Task | What It Does |
|---|---|---|
| **AiExtractWorker** | `dv_ai_extract` | Runs AI/OCR extraction on the document, returning structured fields (name, date, amount, etc.) with a confidence score indicating extraction quality |
| *WAIT task* | `dv_human_verify` | Pauses with the extracted data and confidence score until a human reviewer verifies and corrects the fields, submitting verified data via `POST /tasks/{taskId}` | Built-in Conductor WAIT .  no worker needed |
| **StoreVerifiedWorker** | `dv_store_verified` | Stores the human-verified data as the authoritative record in the document management system or database |

Workers simulate the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments .  the workflow structure stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
dv_ai_extract
    │
    ▼
dv_human_verify [WAIT]
    │
    ▼
dv_store_verified
```

## Example Output

```
=== Document Verification Demo: AI Extract + Human Verify ===

Step 1: Registering task definitions...
  Registered: ...

Step 2: Registering workflow 'document_verification_demo'...
  Workflow registered.

Step 3: Starting workers...
  2 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [dv_ai_extract] Extracting data from document...
  [dv_store_verified] Storing verified document data...

  Status: COMPLETED
  Output: {name=..., dateOfBirth=..., documentNumber=..., expiryDate=...}

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
java -jar target/document-verification-1.0.0.jar
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
java -jar target/document-verification-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow document_verification_demo \
  --version 1 \
  --input '{"documentId": "DOC-001", "DOC-001": "sample-DOC-001"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w document_verification_demo -s COMPLETED -c 5
```

## How to Extend

Each worker covers one side of the extraction-verification loop .  plug in your OCR service (AWS Textract, Google Document AI, ABBYY) for extraction and your system of record for storage, and the verification workflow stays the same.

- **AiExtractWorker** (`dv_ai_extract`): use AWS Textract, Google Document AI, or Azure Form Recognizer for real document data extraction from uploaded images or PDFs
- **StoreVerifiedWorker** (`dv_store_verified`): write verified data to a database, update a customer record in a CRM, or push to a document management system with audit logging

Replace the simulated OCR with Amazon Textract or Google Document AI and the extraction-verification-storage pipeline operates unchanged.

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
document-verification/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/documentverification/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DocumentVerificationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AiExtractWorker.java
│       └── StoreVerifiedWorker.java
└── src/test/java/documentverification/workers/
    ├── AiExtractWorkerTest.java        # 8 tests
    └── StoreVerifiedWorkerTest.java        # 6 tests
```
