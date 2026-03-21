# Bug Triage in Java with Conductor :  Severity-Based Routing for Incoming Bug Reports

A Java Conductor workflow that automatically triages bug reports .  parsing the report text, classifying severity by scanning for keywords like "crash" or "data loss," routing to severity-specific handlers (critical, high, or low), and assigning the bug to the right developer. The workflow uses a SWITCH task to branch execution based on severity, so critical bugs trigger immediate escalation while low-priority issues follow the standard path. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the triage pipeline with conditional routing.

## Routing Bugs by Severity Automatically

When a bug report comes in, someone has to read it, decide how severe it is, and route it accordingly. Critical bugs (crashes, data loss) need immediate attention and different handling than cosmetic issues. Doing this manually is slow and inconsistent .  severity classification depends on who reads the report and when.

This workflow automates the entire triage process. It parses the raw bug report into structured fields (title, description, component), classifies severity by analyzing the description for keywords, then uses a Conductor SWITCH task to route to the appropriate handler: `btg_handle_critical` for crashes and data loss, `btg_handle_high` for errors and broken features, or `btg_handle_low` for everything else. After the severity-specific handling completes, the bug is assigned to a developer based on severity and component.

## The Solution

**You just write the bug-parsing, severity-classification, and assignment workers. Conductor handles the severity-based routing and pipeline sequencing.**

Six workers handle the triage pipeline .  report parsing, severity classification, three severity-specific handlers, and developer assignment. The SWITCH task makes the routing logic declarative: Conductor reads the `severity` output from the classifier and routes to the matching branch automatically. The critical-path handler can page on-call engineers while the low-priority handler simply adds to the backlog ,  each handler is independent and swappable.

### What You Write: Workers

Six workers handle the triage pipeline. ParseReportWorker extracts structured fields, ClassifySeverityWorker assigns a severity level, three handlers process critical/high/low bugs differently, and AssignWorker routes to the right developer.

| Worker | Task | What It Does |
|---|---|---|
| **AssignWorker** | `btg_assign` | Assigns the triaged bug to a developer based on severity and component. |
| **ClassifySeverityWorker** | `btg_classify_severity` | Scans the bug description for keywords (crash, data loss, error) to classify severity as critical, high, or low. |
| **HandleCriticalWorker** | `btg_handle_critical` | Escalates the bug by paging the on-call engineer for immediate response. |
| **HandleHighWorker** | `btg_handle_high` | Flags the bug for inclusion in the next sprint. |
| **HandleLowWorker** | `btg_handle_low` | Adds the bug to the backlog for future prioritization. |
| **ParseReportWorker** | `btg_parse_report` | Parses the raw bug report text into structured fields: title, description, and component. |

Workers implement domain operations .  lead scoring, contact enrichment, deal updates ,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
btg_parse_report
    │
    ▼
btg_classify_severity
    │
    ▼
SWITCH (btg_switch_ref)
    ├── critical: btg_handle_critical
    ├── high: btg_handle_high
    └── default: btg_handle_low
    │
    ▼
btg_assign

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
java -jar target/bug-triage-1.0.0.jar

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
java -jar target/bug-triage-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow btg_bug_triage \
  --version 1 \
  --input '{"bugReport": "sample-bugReport"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w btg_bug_triage -s COMPLETED -c 5

```

## How to Extend

Each worker handles one triage step .  connect your issue tracker (Jira, GitHub Issues, Linear) for parsing and your on-call system (PagerDuty, OpsGenie) for critical escalation, and the triage workflow stays the same.

- **AssignWorker** (`btg_assign`): integrate with Jira or GitHub Issues to auto-assign based on component ownership maps
- **ClassifySeverityWorker** (`btg_classify_severity`): swap keyword matching for an ML classifier or LLM-based severity prediction
- **HandleCriticalWorker** (`btg_handle_critical`): connect to PagerDuty or OpsGenie for immediate on-call escalation

Connect your issue tracker and on-call system and the severity-based triage pipeline operates without any workflow changes.

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
bug-triage/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/bugtriage/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── BugTriageExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssignWorker.java
│       ├── ClassifySeverityWorker.java
│       ├── HandleCriticalWorker.java
│       ├── HandleHighWorker.java
│       ├── HandleLowWorker.java
│       └── ParseReportWorker.java
└── src/test/java/bugtriage/workers/
    ├── AssignWorkerTest.java        # 2 tests
    ├── ClassifySeverityWorkerTest.java        # 2 tests
    ├── HandleCriticalWorkerTest.java        # 2 tests
    ├── HandleHighWorkerTest.java        # 2 tests
    ├── HandleLowWorkerTest.java        # 2 tests
    └── ParseReportWorkerTest.java        # 2 tests

```
