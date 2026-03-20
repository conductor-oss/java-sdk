# Goal Decomposition in Java Using Conductor :  Break Down Goals, Execute Subgoals in Parallel, Aggregate

Goal Decomposition .  decomposes a high-level goal into subgoals, executes them in parallel via FORK/JOIN, then aggregates the results. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Big Goals Need to Be Broken Down

"Improve customer satisfaction" is a goal, not a plan. A useful agent decomposes it into actionable subgoals: analyze current satisfaction scores and identify pain points, benchmark against competitors, and design improvement initiatives. These three subgoals are independent .  they can run simultaneously; but all must complete before the results can be aggregated into a coherent improvement plan.

Decomposition quality determines execution quality. If the subgoals overlap, agents duplicate work. If they have gaps, the final aggregation misses important areas. If one subgoal's agent fails (competitor data API is down), the other two results are still valid .  you just need to retry that one subgoal. Without orchestration, parallel subgoal execution means managing threads, handling partial failures, and synchronizing results manually.

## The Solution

**You write the decomposition, subgoal execution, and aggregation logic. Conductor handles parallel fan-out, independent retries per subgoal, and result synchronization.**

`DecomposeGoalWorker` breaks the high-level goal into three independent subgoals with clear scope, expected outputs, and success criteria. `FORK_JOIN` dispatches `Subgoal1Worker`, `Subgoal2Worker`, and `Subgoal3Worker` to execute each subgoal simultaneously .  each returns structured results with findings and recommendations. After `JOIN` collects all three results, `AggregateWorker` synthesizes the subgoal outputs into a unified plan, resolving any conflicts and identifying cross-cutting themes. Conductor runs all three subgoals in parallel and retries any failed subgoal independently.

### What You Write: Workers

Five workers decompose goals into action. Breaking the goal into subgoals, executing three of them in parallel, and aggregating the results into a unified plan.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `gd_aggregate` | Aggregates the results from all three subgoal workers into a single summary. |
| **DecomposeGoalWorker** | `gd_decompose_goal` | Decomposes a high-level goal into three concrete subgoals. |
| **Subgoal1Worker** | `gd_subgoal_1` | Executes the first subgoal: analyzing current system performance bottlenecks. |
| **Subgoal2Worker** | `gd_subgoal_2` | Executes the second subgoal: researching caching and optimization strategies. |
| **Subgoal3Worker** | `gd_subgoal_3` | Executes the third subgoal: evaluating infrastructure scaling options. |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode .  the agent workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Parallel execution** | FORK_JOIN runs multiple tasks simultaneously and waits for all to complete |

### The Workflow

```
gd_decompose_goal
    │
    ▼
FORK_JOIN
    ├── gd_subgoal_1
    ├── gd_subgoal_2
    └── gd_subgoal_3
    │
    ▼
JOIN (wait for all branches)
gd_aggregate
```

## Example Output

```
=== Goal Decomposition Demo ===

Step 1: Registering task definitions...
  Registered: gd_decompose_goal, gd_subgoal_1, gd_subgoal_2, gd_subgoal_3, gd_aggregate

Step 2: Registering workflow 'goal_decomposition'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [gd_aggregate] Aggregating results for goal:
  [gd_decompose_goal] Decomposing goal:
  [gd_subgoal_1] Executing subgoal
  [gd_subgoal_2] Executing subgoal
  [gd_subgoal_3] Executing subgoal

  Status: COMPLETED
  Output: {aggregatedResult=..., subgoals=..., result=...}

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
java -jar target/goal-decomposition-1.0.0.jar
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
java -jar target/goal-decomposition-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow goal_decomposition \
  --version 1 \
  --input '{"goal": "sample-goal"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w goal_decomposition -s COMPLETED -c 5
```

## How to Extend

Each subgoal worker tackles one independent piece of the problem. Connect real analytics APIs for measurement, web scraping for benchmarking, and LLMs for strategic synthesis, and the decompose-fork-aggregate workflow runs unchanged.

- **DecomposeGoalWorker** (`gd_decompose_goal`): use an LLM to dynamically determine the right number and scope of subgoals based on the goal's complexity, with dependency detection to identify which subgoals can truly run in parallel
- **Subgoal workers** (`gd_subgoal_1/2/3`): connect to real data sources and tools based on the subgoal type: analytics APIs for measurement, web scraping for benchmarking, LLMs for strategic planning
- **AggregateWorker** (`gd_aggregate`): use an LLM to synthesize findings across subgoals, identify conflicts or overlaps, and produce a prioritized action plan with estimated impact and effort

Replace with real LLM decomposition and execution; the parallel subgoal workflow keeps the same decompose-execute-aggregate interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):


## Project Structure

```
goal-decomposition/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/goaldecomposition/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GoalDecompositionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateWorker.java
│       ├── DecomposeGoalWorker.java
│       ├── Subgoal1Worker.java
│       ├── Subgoal2Worker.java
│       └── Subgoal3Worker.java
└── src/test/java/goaldecomposition/workers/
    ├── AggregateWorkerTest.java        # 8 tests
    ├── DecomposeGoalWorkerTest.java        # 8 tests
    ├── Subgoal1WorkerTest.java        # 8 tests
    ├── Subgoal2WorkerTest.java        # 8 tests
    └── Subgoal3WorkerTest.java        # 8 tests
```
