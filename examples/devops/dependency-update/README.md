# Dependency Update in Java with Conductor :  Outdated Scanning, Version Bumping, Test Verification, and PR Creation

Orchestrates automated dependency updates using [Conductor](https://github.com/conductor-oss/conductor). This workflow scans a repository for outdated dependencies, updates them to the latest compatible versions (patch, minor, or major depending on the update type), runs the test suite to verify nothing breaks, and creates a pull request with the changes. You write the update logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Stale Dependency Problem

Your project has 47 dependencies, and 12 of them are behind by at least one minor version. Three have known CVEs. Updating them manually means editing pom.xml or package.json, running tests, finding that one update broke something, reverting it, trying the next one, and eventually giving up because it's Friday. The dependencies stay stale until a security scanner flags them as critical, by which point you're four versions behind and the upgrade path involves breaking changes.

Without orchestration, you'd run `mvn versions:display-dependency-updates`, manually edit version numbers, run `mvn test`, and hope for the best. There's no audit trail of which dependencies were updated, whether tests passed before the PR was created, or which update introduced the failure. If the test run crashes halfway, you'd start over from scratch.

## The Solution

**You write the scanning and update logic. Conductor handles the scan-test-PR pipeline and ensures PRs only open after tests pass.**

Each stage of the dependency update pipeline is a simple, independent worker. The scanner checks the repository for outdated dependencies and identifies available updates based on the configured update type (patch-only for safety, minor for features, major for everything). The updater modifies the dependency manifest files to the target versions. The test runner executes the full test suite against the updated dependencies to catch regressions before anything gets merged. The PR creator opens a pull request with the dependency changes, a summary of what was updated and from/to versions, and the test results. Conductor executes them in strict sequence, ensures the PR is only created if tests pass, retries if the package registry is temporarily unavailable, and tracks how many dependencies were scanned, updated, and verified. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Three workers automate the dependency update cycle. Scanning for outdated packages, bumping versions, and creating pull requests after tests pass.

| Worker | Task | What It Does |
|---|---|---|
| **CreatePrWorker** | `du_create_pr` | Opens a pull request with the dependency changes and a summary diff of updated versions |
| **ScanOutdatedWorker** | `du_scan_outdated` | Scans the repository for outdated dependencies and identifies available updates |
| **UpdateDepsWorker** | `du_update_deps` | Updates dependency manifest files to the target versions (2 major, 3 minor, 3 patch) |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls .  the workflow and rollback logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
du_scan_outdated
    │
    ▼
du_update_deps
    │
    ▼
du_run_tests
    │
    ▼
du_create_pr
```

## Example Output

```
=== Example 334: Dependency Update ===

Step 1: Registering task definitions...
  Registered: du_scan_outdated, du_update_deps, du_run_tests, du_create_pr

Step 2: Registering workflow 'dependency_update_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [pr] Pull request created with dependency diff
  [test] All 142 tests passed after update
  [scan] auth-service: 8 outdated dependencies
  [update] Updated 8 dependencies: 2 major, 3 minor, 3 patch

  Status: COMPLETED

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
java -jar target/dependency-update-1.0.0.jar
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
java -jar target/dependency-update-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow dependency_update_workflow \
  --version 1 \
  --input '{"repository": "sample-repository", "auth-service": "sample-auth-service", "updateType": "standard"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w dependency_update_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one update stage .  replace the simulated calls with Maven Versions Plugin, Snyk, or the GitHub PR API for real dependency scanning and pull request creation, and the update workflow runs unchanged.

- **ScanOutdatedWorker** → scan real dependency manifests: Maven `versions-maven-plugin` for Java, `npm outdated` for Node.js, `pip list --outdated` for Python, or Snyk/Dependabot APIs for vulnerability-aware scanning
- **UpdateDepsWorker** → update real manifests: modify `pom.xml` via Maven Versions Plugin, `package.json` via npm/yarn, or `requirements.txt` via pip-compile, respecting semver constraints
- **RunTestsWorker** → execute real test suites: trigger `mvn test`, `npm test`, or `pytest` via process execution or CI API calls (GitHub Actions, Jenkins), collecting test results and coverage reports
- **CreatePrWorker** → create real pull requests: GitHub API, GitLab MR API, or Bitbucket PR API, with auto-generated descriptions listing each dependency update, version diff, and test results

Wire in Maven or npm APIs for real version scanning; the update pipeline preserves the same data contract.

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
dependency-update-dependency-update/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dependencyupdate/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreatePrWorker.java
│       ├── ScanOutdatedWorker.java
│       └── UpdateDepsWorker.java
└── src/test/java/dependencyupdate/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation
```
