# Title Search in Java with Conductor :  Record Search, Ownership Verification, Lien Check, and Certification

A Java Conductor workflow example for performing property title searches. searching county records for the property's chain of title, verifying current ownership, checking for outstanding liens or encumbrances, and issuing a title certification. Uses [Conductor](https://github.

## The Problem

You need to confirm that a property's title is clear before a sale can close. County records must be searched for the complete chain of title, current ownership must be verified against the recorded deeds, any outstanding liens (tax liens, mechanic's liens, HOA liens, judgments) must be identified, and only if ownership is verified and liens are clear can a title certificate be issued. If the certification step runs before lien checks complete, the buyer risks purchasing a property with hidden encumbrances. Missing a single lien can cost hundreds of thousands of dollars.

Without orchestration, title searches are manual and error-prone. A paralegal searches county records, another person checks for liens, and a title officer issues the certification. all coordinated via email. When the county records system is slow, the search stalls. When a lien check is missed, the title is certified incorrectly. Nobody can tell whether a search is in progress, stuck, or complete.

## The Solution

**You just write the record search, ownership verification, lien check, and title certification logic. Conductor handles lien search retries, ownership verification, and title audit trails.**

Each title search step is a simple, independent worker. one searches county records, one verifies current ownership, one checks for liens, one issues the certification. Conductor takes care of executing them in strict order so no certification is issued without a lien check, retrying if the county records system is temporarily unavailable, and maintaining a complete audit trail that title insurance underwriters can rely on. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Ownership history research, lien search, encumbrance analysis, and title report workers each investigate one aspect of property title status.

| Worker | Task | What It Does |
|---|---|---|
| **SearchRecordsWorker** | `tts_search` | Searches county recorder and assessor records for the property's chain of title |
| **VerifyOwnershipWorker** | `tts_verify_ownership` | Confirms current ownership by matching recorded deeds against the seller's identity |
| **CheckLiensWorker** | `tts_check_liens` | Searches for outstanding liens. tax, mechanic's, HOA, judgment, and federal liens |
| **CertifyTitleWorker** | `tts_certify` | Issues the title certification if ownership is verified and all liens are cleared |

Workers implement property transaction steps. listing, inspection, escrow, closing,  with realistic outputs. Replace with real MLS and escrow service integrations and the workflow stays the same.

### The Workflow

```
tts_search
    │
    ▼
tts_verify_ownership
    │
    ▼
tts_check_liens
    │
    ▼
tts_certify

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
java -jar target/title-search-1.0.0.jar

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
java -jar target/title-search-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tts_title_search \
  --version 1 \
  --input '{"propertyId": "TEST-001", "address": "sample-address", "county": 10}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tts_title_search -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real title systems. county recorder APIs for deed searches, lien search databases for encumbrance checks, your title plant for certification issuance, and the workflow runs identically in production.

- **SearchRecordsWorker** (`tts_search`): query county recorder APIs or title plant databases (DataTrace, TitlePoint) for deed and document history
- **VerifyOwnershipWorker** (`tts_verify_ownership`): cross-reference recorded deeds with tax assessor data and identity verification services
- **CheckLiensWorker** (`tts_check_liens`): search UCC filings, federal tax lien databases, county court records, and HOA records for outstanding encumbrances
- **CertifyTitleWorker** (`tts_certify`): generate a title commitment document, submit to a title insurance underwriter (First American, Fidelity, Old Republic) for final review

Change title databases or search providers and the pipeline adapts with no structural modifications.

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
title-search/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/titlesearch/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TitleSearchExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CertifyTitleWorker.java
│       ├── CheckLiensWorker.java
│       ├── SearchRecordsWorker.java
│       └── VerifyOwnershipWorker.java
└── src/test/java/titlesearch/workers/
    ├── CertifyTitleWorkerTest.java        # 2 tests
    ├── CheckLiensWorkerTest.java        # 2 tests
    ├── SearchRecordsWorkerTest.java        # 2 tests
    └── VerifyOwnershipWorkerTest.java        # 2 tests

```
