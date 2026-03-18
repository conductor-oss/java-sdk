# Github Integration in Java Using Conductor

A critical bug fix gets merged to main at 3 PM. The deployment pipeline should have triggered automatically, but GitHub's webhook delivery failed -- a transient 500 from your CI server that GitHub retried once and then gave up on. Nobody notices. The fix sits undeployed for 4 hours while customers keep hitting the bug, until an engineer happens to check the deploy dashboard and sees the last deploy was from yesterday. The webhook payload is gone, the CI job never ran, and there's no record of why. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the push-to-merge pipeline with durable webhook handling, CI retries, and full audit trails.

## Automating the PR Lifecycle from Push to Merge

When a developer pushes code, the resulting pull request needs to be created, CI checks need to run against it, and if all checks pass, the PR should be merged automatically. Each step depends on the previous one -- you cannot run checks without a PR number, and you cannot merge without knowing the check results. If checks fail, the merge should be skipped.

Without orchestration, you would chain GitHub API calls manually, poll for check status, and manage PR numbers and SHA references between steps. Conductor sequences the pipeline and routes webhook data, PR metadata, and check results between workers automatically.

## The Solution

**You just write the GitHub workers -- webhook reception, PR creation, CI check execution, and merge handling. Conductor handles push-to-merge sequencing, CI check retries, and PR metadata routing between pipeline stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers automate the PR lifecycle: ReceiveWebhookWorker parses push events, CreatePrWorker opens pull requests, RunChecksWorker triggers CI validation, and MergePrWorker merges when all checks pass.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **ReceiveWebhookWorker** | `gh_receive_webhook` | Receives a GitHub webhook push event. | Simulated |
| **CreatePrWorker** | `gh_create_pr` | Creates a pull request on GitHub. | Real (with GitHub API) / Simulated fallback |
| **RunChecksWorker** | `gh_run_checks` | Runs CI checks on the PR. | Simulated |
| **MergePrWorker** | `gh_merge_pr` | Merges the pull request. | Real (with GitHub API) / Simulated fallback |

The workers auto-detect GitHub credentials at startup. When `GITHUB_TOKEN` is set, CreatePrWorker and MergePrWorker use the real GitHub REST API (via `java.net.http`) to create and merge pull requests. Without the token, they fall back to simulated mode with realistic output shapes so the workflow runs end-to-end without a GitHub token.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically -- configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status -- no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
gh_receive_webhook
    │
    ▼
gh_create_pr
    │
    ▼
gh_run_checks
    │
    ▼
gh_merge_pr
```

## Example Output

```
=== Example 434: GitHub Integratio ===

Step 1: Registering task definitions...
  Registered: gh_receive_webhook, gh_create_pr, gh_run_checks, gh_merge_pr

Step 2: Registering workflow 'github_integration_434'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: 8d8c0f55-ca3e-8d93-b706-c89ed2842540

  [webhook] Push to acme/api-service/feature/new-endpoint
  [create_pr] PR #142: feat: add user preferences endpoint
  [create_pr] ERROR: ...
  [create_pr] ERROR: ...
  [create_pr] PR #142: feat: add user preferences endpoint
  [checks] PR #142: lint=passed, unit-tests=passed, build=passed
  [merge] PR #142 merged (checks passed: true)
  [merge] ERROR: ...
  [merge] ERROR: ...
  [merge] PR #142 merged (checks passed: true)

  Status: COMPLETED
  Output: {prNumber=142, checksPassed=true, merged=true}

Result: PASSED
```
## Running It

### Prerequisites

- **Java 21+** -- verify with `java -version`
- **Maven 3.8+** -- verify with `mvn -version`
- **Docker** -- to run Conductor

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
java -jar target/github-integration-1.0.0.jar
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
| `GITHUB_TOKEN` | _(none)_ | GitHub personal access token. When set, enables live PR creation and merging via the GitHub REST API. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/github-integration-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow github_integration_434 \
  --version 1 \
  --input '{"repo": "acme/api-service", "branch": "feature/new-endpoint", "baseBranch": "main", "commitMessage": "add user preferences endpoint"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w github_integration_434 -s COMPLETED -c 5
```

## How to Extend

CreatePrWorker and MergePrWorker already use the real GitHub REST API (via java.net.http) when `GITHUB_TOKEN` is provided. The remaining workers are simulated:

- **ReceiveWebhookWorker** (`gh_receive_webhook`) -- integrate with a real webhook endpoint that receives GitHub push events
- **RunChecksWorker** (`gh_run_checks`) -- integrate with GitHub Actions, Jenkins, or another CI system to run real CI checks against the PR

Replace each simulation with real API calls while keeping the same return schema, and the webhook-to-merge pipeline adapts without changes.

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
github-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/githubintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GithubIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreatePrWorker.java
│       ├── MergePrWorker.java
│       ├── ReceiveWebhookWorker.java
│       └── RunChecksWorker.java
└── src/test/java/githubintegration/workers/
    ├── CreatePrWorkerTest.java        # 2 tests
    ├── MergePrWorkerTest.java        # 2 tests
    ├── ReceiveWebhookWorkerTest.java        # 2 tests
    └── RunChecksWorkerTest.java        # 2 tests
```
