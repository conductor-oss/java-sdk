# Code Review AI in Java with Conductor :  Parallel Security, Quality, and Style Analysis of Pull Requests

A Java Conductor workflow that reviews pull requests automatically .  parsing the diff to extract changed files and line counts, then running security, quality, and style checks in parallel using a FORK_JOIN, and merging the findings into a unified review report with a pass/fail verdict. Given a `prUrl` and `diff`, the pipeline produces issue counts per category, total findings, and an overall verdict. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate parallel code analysis with FORK_JOIN.

## Reviewing Code Across Three Dimensions Simultaneously

Manual code reviews are slow and inconsistent. Security vulnerabilities, code quality issues, and style violations each require different expertise, yet reviewers often focus on one dimension and miss the others. Running these checks sequentially wastes time when they are independent of each other .  a security scan does not need to wait for the style check to finish.

This workflow parses the PR diff once, then fans out to three parallel analysis branches: security (detecting vulnerabilities like weak hash algorithms), quality (identifying complexity, duplication, and maintainability issues), and style (checking formatting and naming conventions). The FORK_JOIN task runs all three simultaneously and waits for all to complete. The report worker then merges findings from all three branches into a single review with total counts, per-category breakdowns, and an overall verdict.

## The Solution

**You just write the diff-parsing, security/quality/style analysis, and report-generation workers. Conductor handles the parallel fan-out and the merged verdict.**

Five workers handle the review pipeline .  diff parsing, security checking, quality checking, style checking, and report generation. The diff parser extracts the file list and lines changed. The three analysis workers run in parallel via FORK_JOIN ,  the security checker looks for vulnerabilities (e.g., MD5 usage in `crypto.js`), the quality checker evaluates complexity and patterns, and the style checker enforces conventions. After all three complete, the report worker aggregates findings and renders the verdict. Conductor manages the parallel fan-out and join automatically.

### What You Write: Workers

ParseDiffWorker extracts file changes, then SecurityCheckWorker, QualityCheckWorker, and StyleCheckWorker run in parallel via FORK_JOIN, and ReportWorker merges the findings into a unified verdict.

| Worker | Task | What It Does |
|---|---|---|
| **ParseDiffWorker** | `cra_parse_diff` | Extracts the file list and line counts (additions/deletions) from the PR diff. |
| **QualityCheckWorker** | `cra_quality_check` | Analyzes code for complexity, duplication, and maintainability issues. |
| **ReportWorker** | `cra_report` | Aggregates findings from all checks into a unified review with total counts and a pass/fail verdict. |
| **SecurityCheckWorker** | `cra_security_check` | Scans for security vulnerabilities (e.g., weak hashing algorithms, injection risks). |
| **StyleCheckWorker** | `cra_style_check` | Checks code formatting, naming conventions, and style guideline adherence. |

Workers implement domain operations .  lead scoring, contact enrichment, deal updates ,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
cra_parse_diff
    │
    ▼
FORK_JOIN
    ├── cra_security_check
    ├── cra_quality_check
    └── cra_style_check
    │
    ▼
JOIN (wait for all branches)
cra_report
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
java -jar target/code-review-ai-1.0.0.jar
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
java -jar target/code-review-ai-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cra_code_review_ai \
  --version 1 \
  --input '{"prUrl": "https://example.com", "diff": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cra_code_review_ai -s COMPLETED -c 5
```

## How to Extend

Each worker handles one analysis dimension .  connect your SAST tool (Snyk, Semgrep) for security, your linter (SonarQube, ESLint) for quality, and your formatter (Prettier, Checkstyle) for style, and the parallel review workflow stays the same.

- **ParseDiffWorker** (`cra_parse_diff`): integrate with the GitHub API to pull real PR diffs and file metadata
- **QualityCheckWorker** (`cra_quality_check`): connect to SonarQube or CodeClimate for real complexity and duplication analysis
- **ReportWorker** (`cra_report`): post review comments directly on the PR via the GitHub Checks API

Connect real static analysis tools (Semgrep, SonarQube, ESLint) and the parallel review with merged verdict keeps working as configured.

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
code-review-ai/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/codereviewai/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CodeReviewAiExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ParseDiffWorker.java
│       ├── QualityCheckWorker.java
│       ├── ReportWorker.java
│       ├── SecurityCheckWorker.java
│       └── StyleCheckWorker.java
└── src/test/java/codereviewai/workers/
    ├── ParseDiffWorkerTest.java        # 2 tests
    ├── QualityCheckWorkerTest.java        # 2 tests
    ├── ReportWorkerTest.java        # 2 tests
    ├── SecurityCheckWorkerTest.java        # 2 tests
    └── StyleCheckWorkerTest.java        # 2 tests
```
