# Multi-Agent Planning in Java Using Conductor :  Architect, Parallel Estimators, Project Manager Timeline

Multi-Agent Project Planning. architect designs the system, three estimators run in parallel (frontend, backend, infra), then PM builds the timeline. Uses [Conductor](https://github.

## Project Planning Needs Multiple Expert Perspectives

A project estimate from a single person is always biased toward their specialty. The backend engineer underestimates frontend work. The infrastructure engineer forgets about API development. An accurate plan needs domain experts estimating their own areas independently. frontend complexity, backend API surface, infrastructure provisioning,  with an architect providing the overall design and a PM resolving the estimates into a realistic timeline.

The three estimation areas are independent and can run simultaneously: the frontend estimator doesn't need to wait for the backend estimate, and vice versa. But all three must complete before the PM can build the timeline. If the infrastructure estimator times out, the frontend and backend estimates are still valid. you just need to retry that one estimation.

## The Solution

**You write the architecture design, domain-specific estimations, and timeline logic. Conductor handles parallel estimation, result collection, and milestone assembly.**

`ArchitectDesignWorker` creates the system design with components, interfaces, and technology choices that guide the estimators. `FORK_JOIN` dispatches three estimators in parallel: `EstimateFrontendWorker` estimates UI components, pages, and client-side complexity. `EstimateBackendWorker` estimates API endpoints, business logic, and integrations. `EstimateInfraWorker` estimates infrastructure provisioning, CI/CD setup, and operational overhead. After `JOIN` collects all three estimates, `PmTimelineWorker` builds a project timeline with milestones, dependencies, critical path identification, and risk buffers. Conductor runs the three estimations in parallel and tracks each estimator's output for estimation accuracy analysis over time.

### What You Write: Workers

Five agents plan the project, the architect designs the system, three estimators run in parallel for frontend, backend, and infrastructure, and the PM assembles the timeline.

| Worker | Task | What It Does |
|---|---|---|
| **ArchitectDesignWorker** | `pp_architect_design` | Architect agent. takes projectName and requirements, produces a system architecture with frontend components, backen.. |
| **EstimateBackendWorker** | `pp_estimate_backend` | Backend estimation agent. takes components, complexity, and teamSize, computes a per-component breakdown (designWeek.. |
| **EstimateFrontendWorker** | `pp_estimate_frontend` | Frontend estimation agent. takes components, complexity, and teamSize, computes a per-component breakdown (designWee.. |
| **EstimateInfraWorker** | `pp_estimate_infra` | Infrastructure estimation agent. takes components, complexity, and teamSize, computes a per-component breakdown (set.. |
| **PmTimelineWorker** | `pp_pm_timeline` | PM timeline agent. takes all three estimates and the architecture summary, computes total project duration = infraCa.. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
pp_architect_design
    │
    ▼
FORK_JOIN
    ├── pp_estimate_frontend
    ├── pp_estimate_backend
    └── pp_estimate_infra
    │
    ▼
JOIN (wait for all branches)
pp_pm_timeline

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
java -jar target/multi-agent-planning-1.0.0.jar

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
java -jar target/multi-agent-planning-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow multi_agent_planning \
  --version 1 \
  --input '{"projectName": "test", "requirements": "sample-requirements"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w multi_agent_planning -s COMPLETED -c 5

```

## How to Extend

Each estimator specializes in one technical domain. Feed in real Jira velocity data for calibration, use LLMs for architecture decisions, and push milestones to project management tools (Asana, Linear), and the design-estimate-timeline workflow runs unchanged.

- **Estimator workers** (`pp_estimate_frontend/backend/infra`): use historical project data to calibrate estimates: query Jira velocity metrics, analyze git commit patterns from similar past projects, or use Monte Carlo simulation for range estimates
- **ArchitectDesignWorker** (`pp_architect_design`): use an LLM with system design pattern catalogs and the project requirements to generate architecture decision records (ADRs) with trade-off analysis
- **PmTimelineWorker** (`pp_pm_timeline`): generate Gantt charts via Mermaid.js, push milestones to Jira/Asana/Linear APIs, and compute critical path using topological sort of task dependencies

Wire in real estimation tools or LLM analysis; the parallel planning pipeline keeps the same architecture-to-timeline contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
multi-agent-planning/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/multiagentplanning/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MultiAgentPlanningExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ArchitectDesignWorker.java
│       ├── EstimateBackendWorker.java
│       ├── EstimateFrontendWorker.java
│       ├── EstimateInfraWorker.java
│       └── PmTimelineWorker.java
└── src/test/java/multiagentplanning/workers/
    ├── ArchitectDesignWorkerTest.java        # 10 tests
    ├── EstimateBackendWorkerTest.java        # 9 tests
    ├── EstimateFrontendWorkerTest.java        # 9 tests
    ├── EstimateInfraWorkerTest.java        # 9 tests
    └── PmTimelineWorkerTest.java        # 10 tests

```
