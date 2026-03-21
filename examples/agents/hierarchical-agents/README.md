# Hierarchical Agents in Java Using Conductor :  Manager, Team Leads, and Workers in a Development Org

Hierarchical agents. manager plans, team leads delegate to workers in parallel branches, manager merges results. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Software Projects Need Hierarchical Coordination

A real development team has a manager who plans the work, team leads who coordinate their teams, and individual contributors who write code. The backend lead assigns API and database tasks; the frontend lead assigns UI and styling tasks. Both teams work simultaneously. The manager reviews everything at the end.

Modeling this hierarchy in an agent system requires multi-level parallelism: the two teams run in parallel, and within each team, the workers run in parallel with their lead. Six agents working on different aspects of the same project need to produce outputs that are consistent and mergeable. Without orchestration, coordinating a 6-agent hierarchy with two levels of parallelism, proper synchronization at each level, and a final merge step requires complex concurrency code.

## The Solution

**You write the manager, lead, and developer agent logic. Conductor handles nested parallelism, cross-team synchronization, and output merging.**

`ManagerPlanWorker` creates the project plan with feature requirements and team assignments. `FORK_JOIN` launches two parallel branches: the backend branch runs `LeadBackendWorker` (coordination) followed by `WorkerApiWorker` and `WorkerDbWorker` (implementation), while the frontend branch runs `LeadFrontendWorker` (coordination) followed by `WorkerUiWorker` and `WorkerStylingWorker` (implementation). After `JOIN` synchronizes both teams, `ManagerMergeWorker` reviews all outputs, checks for integration issues between frontend and backend, and produces the final deliverable. Conductor handles the nested parallelism and tracks each agent's contribution.

### What You Write: Workers

Eight agents form a hierarchy, the manager plans, team leads delegate to frontend and backend developers in parallel branches, and the manager merges all outputs.

| Worker | Task | What It Does |
|---|---|---|
| **LeadBackendWorker** | `hier_lead_backend` | Backend team lead that receives the backend workstream and breaks it down into an API task and a database task for th... |
| **LeadFrontendWorker** | `hier_lead_frontend` | Frontend team lead that receives the frontend workstream and breaks it down into a UI task and a styling task for the... |
| **ManagerMergeWorker** | `hier_manager_merge` | Manager agent that merges backend and frontend results into a final project report, computing total lines of code acr... |
| **ManagerPlanWorker** | `hier_manager_plan` | Manager agent that takes a project description and deadline, then produces a backend plan and a frontend plan for the... |
| **WorkerApiWorker** | `hier_worker_api` | API developer worker that implements the REST endpoints defined by the backend lead. |
| **WorkerDbWorker** | `hier_worker_db` | Database developer worker that builds the schema and migrations based on the DB task from the lead and the API output. |
| **WorkerStylingWorker** | `hier_worker_styling` | Styling developer worker that applies the design system, responsive layout, and theme support based on the styling ta... |
| **WorkerUiWorker** | `hier_worker_ui` | UI developer worker that builds the page components defined by the frontend lead. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
hier_manager_plan
    │
    ▼
FORK_JOIN
    ├── hier_lead_backend -> hier_worker_api -> hier_worker_db
    └── hier_lead_frontend -> hier_worker_ui -> hier_worker_styling
    │
    ▼
JOIN (wait for all branches)
hier_manager_merge

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
java -jar target/hierarchical-agents-1.0.0.jar

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
java -jar target/hierarchical-agents-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow hierarchical_agents \
  --version 1 \
  --input '{"project": "sample-project", "deadline": "sample-deadline"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w hierarchical_agents -s COMPLETED -c 5

```

## How to Extend

Each agent in the hierarchy is self-contained. Connect code generation APIs (Codex, Copilot) for developer workers, LLMs for lead coordination, and integration testing tools for the manager review, and the plan-fork-merge organizational workflow runs unchanged.

- **Worker agents** (`hier_worker_api/db/ui/styling`): connect to real code generation APIs (Codex, Copilot) with repository context, or trigger actual CI/CD pipelines for code generation and testing
- **Lead agents** (`hier_lead_backend/frontend`): use LLMs with architectural context to produce detailed technical specifications that feed into worker task descriptions
- **ManagerMergeWorker** (`hier_manager_merge`): use an LLM to detect integration issues between frontend and backend outputs (API contract mismatches, missing error handling) and generate integration test specifications

Swap in real code generation tools; the hierarchical pipeline maintains the same plan-delegate-merge contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
hierarchical-agents/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/hierarchicalagents/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── HierarchicalAgentsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── LeadBackendWorker.java
│       ├── LeadFrontendWorker.java
│       ├── ManagerMergeWorker.java
│       ├── ManagerPlanWorker.java
│       ├── WorkerApiWorker.java
│       ├── WorkerDbWorker.java
│       ├── WorkerStylingWorker.java
│       └── WorkerUiWorker.java
└── src/test/java/hierarchicalagents/workers/
    ├── LeadBackendWorkerTest.java        # 6 tests
    ├── LeadFrontendWorkerTest.java        # 6 tests
    ├── ManagerMergeWorkerTest.java        # 6 tests
    ├── ManagerPlanWorkerTest.java        # 6 tests
    ├── WorkerApiWorkerTest.java        # 5 tests
    ├── WorkerDbWorkerTest.java        # 5 tests
    ├── WorkerStylingWorkerTest.java        # 5 tests
    └── WorkerUiWorkerTest.java        # 5 tests

```
