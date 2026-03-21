# Tree of Thought in Java Using Conductor :  Explore Three Solution Paths in Parallel, Evaluate, Select Best

Tree of Thought. define a problem, explore three parallel reasoning paths (analytical, creative, empirical), evaluate all paths, and select the best solution. Uses [Conductor](https://github.

## Some Problems Have Multiple Solution Approaches

"How should we reduce cloud infrastructure costs by 30%?" has at least three valid approaches: right-sizing instances (analyze usage, downsize over-provisioned resources), reserved capacity (commit to 1-3 year reservations for predictable workloads), or architectural changes (move to serverless, consolidate services). Each approach has different risk profiles, implementation timelines, and savings potential.

Chain-of-thought picks one path and follows it. Tree-of-thought explores all three simultaneously and compares them. Path A might save 15% with low risk in 2 weeks. Path B might save 35% with medium risk in 3 months. Path C might save 40% with high risk in 6 months. Only by exploring all three can you make an informed decision. Without orchestration, parallel path exploration means managing concurrent LLM calls, collecting heterogeneous results, and comparing them consistently.

## The Solution

**You write the problem definition, path exploration, and evaluation logic. Conductor handles parallel path execution, consistent scoring, and comparative selection.**

`DefineProblemWorker` analyzes the problem and identifies the key constraints, success criteria, and evaluation metrics. `FORK_JOIN` dispatches three path explorers: `PathAWorker`, `PathBWorker`, and `PathCWorker` each develop a complete solution approach with rationale, estimated impact, risks, and implementation plan. After `JOIN` collects all three paths, `EvaluatePathsWorker` scores each path against the defined criteria using a consistent rubric. `SelectBestWorker` picks the highest-scoring path and produces the final recommendation with the comparative analysis. Conductor runs all three explorations in parallel and records each path's development and scoring.

### What You Write: Workers

Six workers explore solution space. Defining the problem, exploring three parallel reasoning paths (analytical, creative, empirical), evaluating all paths, and selecting the best.

| Worker | Task | What It Does |
|---|---|---|
| **DefineProblemWorker** | `tt_define_problem` | Defines Problem and computes problem |
| **EvaluatePathsWorker** | `tt_evaluate_paths` | Evaluates and computes scores, best path, best solution, evaluation |
| **PathAWorker** | `tt_path_a` | Analytical reasoning path. Proposes a conventional, well-proven solution using load balancers, auto-scaling groups, a... |
| **PathBWorker** | `tt_path_b` | Creative reasoning path. Proposes an innovative edge-computing approach using CDN-based logic, serverless edge functi... |
| **PathCWorker** | `tt_path_c` | Empirical reasoning path. Proposes a data-driven solution based on traffic analysis: regional clusters with geo-routi... |
| **SelectBestWorker** | `tt_select_best` | Selects and returns the best reasoning path and its solution as the final output of the tree-of-thought workflow. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
tt_define_problem
    │
    ▼
FORK_JOIN
    ├── tt_path_a
    ├── tt_path_b
    └── tt_path_c
    │
    ▼
JOIN (wait for all branches)
tt_evaluate_paths
    │
    ▼
tt_select_best

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
java -jar target/tree-of-thought-1.0.0.jar

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
java -jar target/tree-of-thought-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tree_of_thought \
  --version 1 \
  --input '{"problem": "sample-problem"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tree_of_thought -s COMPLETED -c 5

```

## How to Extend

Each path worker explores one solution approach. Use different LLM configurations or providers (GPT-4, Claude, Gemini) per path for genuine diversity, add a weighted multi-criteria evaluator, and the define-fork-evaluate-select workflow runs unchanged.

- **Path workers** (`tt_path_a/b/c`): use different LLM configurations (temperature, system prompts) per path to generate genuinely diverse approaches, or use different models (GPT-4, Claude, Gemini) for each path
- **EvaluatePathsWorker** (`tt_evaluate_paths`): implement weighted multi-criteria evaluation with configurable weights per criterion, or use a separate LLM as an impartial evaluator
- **SelectBestWorker** (`tt_select_best`): implement ensemble selection: instead of picking one path, synthesize the best elements from all three into a hybrid approach using an LLM-based merger

Replace with real LLM reasoning per path; the parallel exploration pipeline maintains the same define-explore-evaluate-select interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
tree-of-thought/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/treeofthought/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TreeOfThoughtExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DefineProblemWorker.java
│       ├── EvaluatePathsWorker.java
│       ├── PathAWorker.java
│       ├── PathBWorker.java
│       ├── PathCWorker.java
│       └── SelectBestWorker.java
└── src/test/java/treeofthought/workers/
    ├── DefineProblemWorkerTest.java        # 8 tests
    ├── EvaluatePathsWorkerTest.java        # 10 tests
    ├── PathAWorkerTest.java        # 9 tests
    ├── PathBWorkerTest.java        # 9 tests
    ├── PathCWorkerTest.java        # 9 tests
    └── SelectBestWorkerTest.java        # 9 tests

```
