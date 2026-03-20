# Commit Analysis in Java with Conductor :  Parse, Classify, and Detect Patterns in Git History

A Java Conductor workflow that analyzes a repository's commit history .  parsing commits from a branch over a configurable time window, classifying each commit by type (feature, bugfix, refactor, etc.), detecting development patterns across the classified commits, and generating a summary report. Given a `repoName`, `branch`, and `days`, the pipeline produces commit counts, type breakdowns, detected patterns, and a narrative report. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the four-step analysis pipeline.

## Understanding What Your Team Has Been Building

Raw git log output is a wall of hashes and messages. To understand development trends .  whether the team is spending more time on bug fixes than features, whether refactoring is keeping pace with new code, or whether certain components are seeing unusual churn ,  you need to parse, classify, and aggregate commits systematically.

This workflow processes a repository's recent history in four steps. The parser extracts commits from the specified branch and time window. The classifier categorizes each commit by type (feature, bugfix, refactor, chore, etc.) and produces a type distribution summary. The pattern detector analyzes the classified commits for trends like "increasing bug density in module X" or "refactoring sprint in week 3." The reporter compiles patterns and classifications into a readable summary.

## The Solution

**You just write the commit-parsing, classification, pattern-detection, and reporting workers. Conductor handles the analysis pipeline and data routing.**

Four workers form the analysis pipeline .  commit parsing, classification, pattern detection, and reporting. The parser reads the git history for the specified branch and time range. The classifier labels each commit and produces a summary distribution. The pattern detector examines the classified data for trends and anomalies. The reporter merges patterns and classifications into a final report. Conductor sequences the steps and routes commits, classifications, and patterns between them via JSONPath.

### What You Write: Workers

ParseCommitsWorker reads git history, ClassifyWorker labels each commit by type, DetectPatternsWorker finds trends like bug density spikes, and ReportWorker compiles the narrative analysis.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyWorker** | `cma_classify` | Labels each commit by type (feature, bugfix, refactor, chore) and produces a type distribution summary. |
| **DetectPatternsWorker** | `cma_detect_patterns` | Analyzes classified commits to identify development patterns and trends (e.g., bug density spikes, refactoring sprints). |
| **ParseCommitsWorker** | `cma_parse_commits` | Extracts commits from the specified branch and time window with hashes, authors, and messages. |
| **ReportWorker** | `cma_report` | Compiles patterns and classifications into a narrative analysis report. |

Workers simulate CRM operations .  lead scoring, contact enrichment, deal updates ,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
cma_parse_commits
    │
    ▼
cma_classify
    │
    ▼
cma_detect_patterns
    │
    ▼
cma_report
```

## Example Output

```
=== Example 646: Commit Analysis ===

Step 1: Registering task definitions...
  Registered: cma_parse_commits, cma_classify, cma_detect_patterns, cma_report

Step 2: Registering workflow 'cma_commit_analysis'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [classify] Features:
  [patterns] Detected
  [parse] Found
  [report] Generated analysis report with

  Status: COMPLETED
  Output: {features=..., fixes=..., refactors=..., classified=...}

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
java -jar target/commit-analysis-1.0.0.jar
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
java -jar target/commit-analysis-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cma_commit_analysis \
  --version 1 \
  --input '{"repoName": "sample-name", "acme/platform": "sample-acme/platform", "branch": "sample-branch", "main": "sample-main", "days": "sample-days"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cma_commit_analysis -s COMPLETED -c 5
```

## How to Extend

Each worker handles one analysis step .  connect your Git hosting API (GitHub, GitLab, Bitbucket) for commit parsing and your team dashboard (Sleuth, LinearB) for trend reporting, and the commit-analysis workflow stays the same.

- **ClassifyWorker** (`cma_classify`): use an LLM or Conventional Commits parser to classify commits more accurately than keyword matching
- **DetectPatternsWorker** (`cma_detect_patterns`): integrate with analytics platforms (Grafana, Datadog) to correlate commit patterns with deployment incidents
- **ParseCommitsWorker** (`cma_parse_commits`): connect to the GitHub or GitLab API to pull real commit history instead of simulated data

Integrate the GitHub or GitLab API for real commit data and the analysis pipeline with pattern detection stays intact.

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
commit-analysis/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/commitanalysis/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CommitAnalysisExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClassifyWorker.java
│       ├── DetectPatternsWorker.java
│       ├── ParseCommitsWorker.java
│       └── ReportWorker.java
└── src/test/java/commitanalysis/workers/
    ├── ClassifyWorkerTest.java        # 2 tests
    ├── DetectPatternsWorkerTest.java        # 2 tests
    ├── ParseCommitsWorkerTest.java        # 2 tests
    └── ReportWorkerTest.java        # 2 tests
```
