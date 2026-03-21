# Trademark Search in Java with Conductor

A Java Conductor workflow example demonstrating Trademark Search. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

A business team wants to launch a new product under a proposed brand name. Before committing to marketing spend, you need to search trademark databases for existing registrations, analyze any conflicts (e.g., a mark with 65% similarity), assess the overall registration risk level, and recommend whether to proceed, modify, or abandon the name. Skipping this step can result in cease-and-desist letters, costly rebranding, or trademark infringement litigation.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the mark search, similarity analysis, conflict detection, and clearance recommendation logic. Conductor handles search retries, conflict analysis, and clearance audit trails.**

Each worker handles one legal operation. Conductor manages the review pipeline, approval chains, deadline enforcement, and audit trail.

### What You Write: Workers

Mark analysis, database search, conflict identification, and availability report workers each own one stage of the trademark clearance process.

| Worker | Task | What It Does |
|---|---|---|
| **SearchWorker** | `tmk_search` | Searches trademark databases for the proposed name, returning matching marks (e.g., SimilarMark-001, SimilarMark-002) and the total result count |
| **ConflictsWorker** | `tmk_conflicts` | Analyzes search results for direct conflicts, computing similarity scores (e.g., 0.65 for SimilarMark-001) and reporting the total conflict count |
| **AssessWorker** | `tmk_assess` | Evaluates overall trademark registration risk based on conflicts, returning a risk level (e.g., "moderate") and an assessment summary |
| **RecommendWorker** | `tmk_recommend` | Generates a go/no-go recommendation (e.g., "proceed-with-caution") and suggests alternative names (e.g., {name}Plus, {name}Pro) if conflicts exist |

Workers implement legal operations. document review, compliance checks, approval routing,  with realistic outputs. Replace with real document management and e-signature integrations and the workflow stays the same.

### The Workflow

```
tmk_search
    │
    ▼
tmk_conflicts
    │
    ▼
tmk_assess
    │
    ▼
tmk_recommend

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
java -jar target/trademark-search-1.0.0.jar

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
java -jar target/trademark-search-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tmk_trademark_search \
  --version 1 \
  --input '{"trademarkName": "test", "goodsAndServices": "order-service"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tmk_trademark_search -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real trademark tools. the USPTO TESS database for mark searches, your similarity scoring engine for conflict detection, your legal review platform for clearance opinions, and the workflow runs identically in production.

- **SearchWorker** (`tmk_search`): query the USPTO TESS database, WIPO Global Brand Database, or commercial services like TrademarkNow or Corsearch for registered and pending marks
- **ConflictsWorker** (`tmk_conflicts`): use a trademark similarity engine like CompuMark or Corsearch to compute phonetic, visual, and conceptual similarity scores against found marks
- **AssessWorker** (`tmk_assess`): integrate with a legal risk model or IP counsel review workflow to evaluate likelihood of confusion under the DuPont factors
- **RecommendWorker** (`tmk_recommend`): generate alternative name suggestions using branding tools or AI, and push the recommendation report to stakeholders via Slack, email, or an IP management platform like Anaqua

Change search databases or conflict analysis rules and the clearance pipeline adapts transparently.

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
trademark-search/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/trademarksearch/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TrademarkSearchExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssessWorker.java
│       ├── ConflictsWorker.java
│       ├── RecommendWorker.java
│       └── SearchWorker.java
└── src/test/java/trademarksearch/workers/
    ├── AssessWorkerTest.java        # 2 tests
    ├── ConflictsWorkerTest.java        # 2 tests
    ├── RecommendWorkerTest.java        # 2 tests
    └── SearchWorkerTest.java        # 2 tests

```
