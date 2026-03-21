# PR Review AI in Java with Conductor :  Fetch, Analyze, Review, and Post Comments on Pull Requests

A Java Conductor workflow that automates pull request reviews .  fetching the diff from the repository, analyzing the changes for issues and patterns, generating a review with line-level comments, and posting the review back to the PR. Given a `repoName` and `prNumber`, the pipeline produces a diff analysis, review comments, and a posted review status. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the fetch-analyze-generate-post review pipeline.

## Giving Every PR a Thorough Review Automatically

Code reviews are essential but time-consuming. Reviewers miss things when they are tired, rushed, or unfamiliar with the codebase. An AI reviewer provides consistent, thorough feedback on every PR .  catching patterns like missing error handling, security concerns, or style inconsistencies that humans might overlook. The AI review does not replace human reviewers; it gives them a head start by flagging issues before they even open the PR.

This workflow processes one pull request. The diff fetcher retrieves the PR changes from the repository. The change analyzer examines the diff for patterns, complexity, and potential issues. The review generator produces a structured review with specific comments tied to files and line numbers. The poster submits the review as comments on the PR. Each step's output feeds the next .  the raw diff feeds analysis, the analysis feeds review generation, and the generated review feeds posting.

## The Solution

**You just write the diff-fetching, change-analysis, review-generation, and posting workers. Conductor handles the review pipeline and PR data flow.**

Four workers handle the review pipeline .  diff fetching, change analysis, review generation, and review posting. The fetcher pulls the PR diff from the repository. The analyzer examines changes for patterns and issues. The generator creates review comments with file and line references. The poster submits the review to the PR. Conductor sequences the four steps and passes diffs, analyses, and review comments between them via JSONPath.

### What You Write: Workers

FetchDiffWorker retrieves the PR changes, AnalyzeChangesWorker examines patterns and complexity, GenerateReviewWorker produces line-level comments, and PostReviewWorker submits the review to the PR.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeChangesWorker** | `prr_analyze_changes` | Examines the parsed diff for code issues, complexity, and patterns; outputs an issue list and complexity rating. |
| **FetchDiffWorker** | `prr_fetch_diff` | Retrieves the PR diff from the repository, returning changed files, additions, and deletions. |
| **GenerateReviewWorker** | `prr_generate_review` | Produces a structured review with file- and line-level comments and an overall verdict (approve/request changes). |
| **PostReviewWorker** | `prr_post_review` | Posts the generated review as comments on the pull request. |

Workers implement domain operations .  lead scoring, contact enrichment, deal updates ,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
prr_fetch_diff
    │
    ▼
prr_analyze_changes
    │
    ▼
prr_generate_review
    │
    ▼
prr_post_review

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
java -jar target/pr-review-ai-1.0.0.jar

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
java -jar target/pr-review-ai-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow prr_pr_review \
  --version 1 \
  --input '{"repoName": "test", "prNumber": "sample-prNumber"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w prr_pr_review -s COMPLETED -c 5

```

## How to Extend

Each worker handles one review step .  connect your Git hosting API (GitHub, GitLab, Bitbucket) for diff fetching and your LLM (Claude, GPT-4) for review generation, and the PR-review workflow stays the same.

- **AnalyzeChangesWorker** (`prr_analyze_changes`): use an LLM (GPT-4, Claude) for deep semantic analysis of code changes
- **FetchDiffWorker** (`prr_fetch_diff`): integrate with the GitHub or GitLab REST API to fetch real PR diffs
- **GenerateReviewWorker** (`prr_generate_review`): swap in an LLM for context-aware review comment generation

Connect the GitHub API and your AI model and the fetch-analyze-review-post pipeline continues to review PRs without workflow changes.

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
pr-review-ai/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/prreviewai/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PrReviewAiExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeChangesWorker.java
│       ├── FetchDiffWorker.java
│       ├── GenerateReviewWorker.java
│       └── PostReviewWorker.java
└── src/test/java/prreviewai/workers/
    ├── AnalyzeChangesWorkerTest.java        # 2 tests
    ├── FetchDiffWorkerTest.java        # 2 tests
    ├── GenerateReviewWorkerTest.java        # 2 tests
    └── PostReviewWorkerTest.java        # 2 tests

```
