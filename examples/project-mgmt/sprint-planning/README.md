# Sprint Planning Automation in Java with Conductor :  Story Selection, Estimation, Assignment, and Sprint Creation

A Java Conductor workflow example that automates sprint planning .  selecting user stories from the backlog based on team capacity, estimating story points for each selected story, assigning stories to individual team members, and creating the sprint with total point commitment and active status. Uses [Conductor](https://github.

## Why Sprint Planning Needs Orchestration

Planning a sprint requires a sequence where each decision constrains the next. You select stories from the prioritized backlog that fit the team's capacity .  pulling high-priority items first (US-101 "User login"), then medium (US-102 "Dashboard view"), then low (US-103 "Export CSV") until you approach the capacity limit. You estimate each selected story in points ,  5 points for the login feature, 8 for the dashboard, 3 for CSV export ,  producing a total commitment of 16 points. You assign each estimated story to a team member based on skills and individual capacity. Alice takes US-101, Bob takes US-102, Carol takes US-103. Finally, you create the sprint ,  recording the sprint number, story count, total points, and setting the status to ACTIVE.

Each step depends on the previous one .  you cannot estimate stories you have not selected, you cannot assign stories without knowing their point values (a 3-point story and an 8-point story require different capacity from the assignee), and you cannot create the sprint without finalized assignments. If estimation reveals that the selected stories exceed capacity (e.g., 16 points selected for a 15-point team), you need to drop the lowest-priority story ,  not re-select from the backlog. Without orchestration, you'd build a monolithic planning script that mixes backlog queries, estimation logic, team availability lookups, and sprint creation ,  making it impossible to swap your estimation method (planning poker vs: t-shirt sizing), retry a failed Jira API call without re-estimating, or audit why a specific story was assigned to a specific developer.

## How This Workflow Solves It

**You just write the story selection, estimation, developer assignment, and sprint creation logic. Conductor handles capacity retries, task assignment sequencing, and sprint audit trails.**

Each sprint planning stage is an independent worker .  select stories, estimate, assign, create sprint. Conductor sequences them, passes the selected stories into estimation, feeds the estimated stories into assignment, hands the finalized assignments to sprint creation, retries if your project management tool's API times out during story selection, and records every planning decision for retrospective analysis.

### What You Write: Workers

Backlog analysis, capacity calculation, sprint goal setting, and task assignment workers each handle one aspect of iteration planning.

| Worker | Task | What It Does |
|---|---|---|
| **AssignWorker** | `spn_assign` | Assigning stories to team members |
| **CreateSprintWorker** | `spn_create_sprint` | Creates the sprint |
| **EstimateWorker** | `spn_estimate` | Estimating stories |
| **SelectStoriesWorker** | `spn_select_stories` | Selecting stories for capacity |

Workers simulate project management operations .  task creation, status updates, notifications ,  with realistic outputs. Replace with real Jira/Asana/Linear integrations and the workflow stays the same.

### The Workflow

```
spn_select_stories
    │
    ▼
spn_estimate
    │
    ▼
spn_assign
    │
    ▼
spn_create_sprint

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
java -jar target/sprint-planning-1.0.0.jar

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
java -jar target/sprint-planning-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow sprint_planning_sprint-planning \
  --version 1 \
  --input '{"sprintNumber": 5, "teamCapacity": "sample-teamCapacity"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w sprint_planning_sprint-planning -s COMPLETED -c 5

```

## How to Extend

Wire each worker to your real agile tools. Jira for backlog queries and story selection, your estimation platform for point sizing, your capacity tracker for team assignments, and the workflow runs identically in production.

- **SelectStoriesWorker** (`spn_select_stories`): query your backlog via the Jira, Linear, or Shortcut API, filter by priority and labels, and select stories that fit within the team's velocity-based capacity
- **EstimateWorker** (`spn_estimate`): pull existing estimates from your PM tool or trigger an estimation workflow (planning poker via Parabol, async estimation via Slack bot), returning point values and a total commitment
- **AssignWorker** (`spn_assign`): match stories to team members based on skill tags, current workload from your resource management system, and individual capacity remaining in the sprint
- **CreateSprintWorker** (`spn_create_sprint`): create the sprint in Jira, Linear, or your PM tool via its API, move the assigned stories into the sprint, set start/end dates, and activate it

Swap your backlog tool or capacity model and the planning pipeline adjusts transparently.

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
sprint-planning-sprint-planning/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/sprintplanning/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SprintPlanningExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AssignWorker.java
│       ├── CreateSprintWorker.java
│       ├── EstimateWorker.java
│       └── SelectStoriesWorker.java
└── src/test/java/sprintplanning/workers/
    ├── AssignWorkerTest.java        # 2 tests
    ├── CreateSprintWorkerTest.java        # 2 tests
    ├── EstimateWorkerTest.java        # 2 tests
    └── SelectStoriesWorkerTest.java        # 2 tests

```
