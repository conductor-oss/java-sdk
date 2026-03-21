# Jira Integration in Java Using Conductor

Your sprint board shows 47 tickets "In Progress." The standup takes 25 minutes because nobody can tell which ones are actually being worked on. Three of those bugs were fixed last week: the engineers pushed the code, closed the PRs, and forgot to update Jira. Eight more are blocked but still show "In Progress" because the blocked status requires a manual transition that nobody remembers to do. The PM pulls a velocity report for the stakeholder meeting and it says the team completed 4 story points this sprint, when they actually shipped 22. The data is useless because ticket status and actual work diverged weeks ago. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the Jira issue lifecycle, creation, transitions, tracking, and notifications, as a durable pipeline.

## Automating Jira Issue Lifecycle

When an event triggers issue creation (e.g., a deployment, a bug report, a feature request), the issue needs to be created in Jira, transitioned through workflow statuses, tracked for updates, and the assignee notified of each change. Each step depends on the previous one. You cannot transition an issue before creating it, and you cannot notify about a status change before the transition happens.

Without orchestration, you would chain Jira REST API calls manually, manage issue keys between steps, and build custom notification logic. Conductor sequences the pipeline and routes issue keys, statuses, and assignee information between workers automatically.

## The Solution

**You just write the Jira workers. Issue creation, status transitions, tracking, and assignee notification. Conductor handles create-to-notify sequencing, Jira API retries, and issue key routing between transition and tracking stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers manage the issue lifecycle: CreateIssueWorker opens tickets, TransitionWorker moves issues through statuses, TrackStatusWorker monitors current state, and NotifyWorker alerts assignees of changes.

| Worker | Task | What It Does |
|---|---|---|
| **CreateIssueWorker** | `jra_create_issue` | Creates a Jira issue via the Jira REST API (POST /rest/api/3/issue). When `JIRA_URL` and `JIRA_API_TOKEN` are set, calls the real Jira API. When unset, runs in simulated mode with deterministic issue keys. |
| **TransitionWorker** | `jra_transition` | Transitions the issue. moves the issue through workflow statuses (To Do -> In Progress -> Done) and returns the new status |
| **TrackStatusWorker** | `jra_track_status` | Tracks the issue status: queries the current status, assignee, and last update timestamp for the issue |
| **NotifyWorker** | `jra_notify` | Notifies the assignee. sends a notification about the status change with the issue key and new status |

Workers implement external API calls with realistic response shapes so you can see the integration flow end-to-end. Replace with real API clients, the workflow orchestration and error handling stay the same.

### The Workflow

```
Input -> CreateIssueWorker -> NotifyWorker -> TrackStatusWorker -> TransitionWorker -> Output

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
java -jar target/jira-integration-1.0.0.jar

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
| `JIRA_URL` | _(none)_ | Jira instance URL (e.g., `https://yoursite.atlassian.net`). Required for live mode. |
| `JIRA_EMAIL` | _(none)_ | Jira account email for Basic auth. Required for live mode. |
| `JIRA_API_TOKEN` | _(none)_ | Jira API token. When set with `JIRA_URL`, CreateIssueWorker calls the Jira REST API. When unset, all workers run in simulated mode with `` output prefix. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/jira-integration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow jira_integration \
  --version 1 \
  --input '{"project": "ENG", "summary": "Fix login timeout issue", "description": "Users experiencing timeout on login page", "assignee": "jane.doe"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w jira_integration -s COMPLETED -c 5

```

## How to Extend

Swap in Jira REST API calls. POST /rest/api/3/issue for creation, POST /transitions for status changes, and your notification channel (Slack, email) for alerts. The workflow definition stays exactly the same.

- **CreateIssueWorker** (`jra_create_issue`): use the Jira REST API to create real issues with project, summary, and assignee fields
- **TransitionWorker** (`jra_transition`): use the Jira REST API transitions endpoint to move issues between workflow statuses
- **NotifyWorker** (`jra_notify`): integrate with Slack, email, or Jira's own notification system for real assignee alerts

Swap each simulation for real Jira REST API calls while preserving output fields, and the issue lifecycle pipeline stays unchanged.

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
jira-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/jiraintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── JiraIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreateIssueWorker.java
│       ├── NotifyWorker.java
│       ├── TrackStatusWorker.java
│       └── TransitionWorker.java
└── src/test/java/jiraintegration/workers/
    ├── CreateIssueWorkerTest.java        # 8 tests
    ├── NotifyWorkerTest.java        # 8 tests
    ├── TrackStatusWorkerTest.java        # 7 tests
    └── TransitionWorkerTest.java        # 8 tests

```
