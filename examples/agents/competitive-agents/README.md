# Competitive Agents in Java Using Conductor :  Parallel Solvers, Judge, and Winner Selection

Competitive Agents .  three solvers propose solutions in parallel, a judge scores them, and a winner is selected. Uses [Conductor](https://github.

## Better Solutions Through Competition

A single AI agent solving a problem gives you one perspective. Three agents solving the same problem independently give you three perspectives, and the best one is almost always better than a random single attempt. Agent 1 might take an analytical approach (data-driven, quantitative). Agent 2 might take a creative approach (novel angles, unconventional solutions). Agent 3 might take a practical approach (implementation-focused, risk-aware).

The challenge is running all three simultaneously (sequential execution triples the latency), evaluating their solutions objectively against the same criteria, and selecting the winner with justification. Without orchestration, parallel agent execution means managing thread pools, handling partial failures (agent 2 timed out but agents 1 and 3 produced solutions), and synchronizing results for the judge.

## The Solution

**You write the solver strategies and judging criteria. Conductor handles parallel execution, result collection, and comparative scoring.**

`FORK_JOIN` dispatches `Solver1Worker`, `Solver2Worker`, and `Solver3Worker` to tackle the problem simultaneously .  each takes a different approach (analytical, creative, practical) and produces a solution with rationale. After `JOIN` collects all three solutions, `JudgeAgentWorker` evaluates each against the specified criteria (accuracy, creativity, feasibility, clarity) and assigns scores. `SelectWinnerWorker` picks the highest-scoring solution and produces the final output with the winning solution, scores for all three, and the judge's reasoning. Conductor runs all three solvers in parallel, so total time equals the slowest solver, not the sum of all three.

### What You Write: Workers

Five workers run the competition. Three solvers propose solutions in parallel using different strategies, a judge scores them, and a winner is selected.

| Worker | Task | What It Does |
|---|---|---|
| **JudgeAgentWorker** | `comp_judge_agent` | Judge agent .  evaluates all three solver solutions, scores each on cost, innovation, and risk, then ranks them to det.. |
| **SelectWinnerWorker** | `comp_select_winner` | Select winner .  takes the judge's judgment and all solutions, produces a winner map and a ranked list of all solutions. |
| **Solver1Worker** | `comp_solver_1` | Creative solver .  proposes an innovative, AI-powered adaptive solution. Uses a "creative" approach with higher innova.. |
| **Solver2Worker** | `comp_solver_2` | Analytical solver .  proposes a data-driven optimization framework. Uses an "analytical" approach with balanced innova.. |
| **Solver3Worker** | `comp_solver_3` | Practical solver .  proposes an incremental process improvement. Uses a "practical" approach with low cost, low risk.. |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode .  the agent workflow stays the same.

### The Workflow

```
FORK_JOIN
    ├── comp_solver_1
    ├── comp_solver_2
    └── comp_solver_3
    │
    ▼
JOIN (wait for all branches)
comp_judge_agent
    │
    ▼
comp_select_winner

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
java -jar target/competitive-agents-1.0.0.jar

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
java -jar target/competitive-agents-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow competitive_agents_demo \
  --version 1 \
  --input '{"problem": "sample-problem", "criteria": "sample-criteria"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w competitive_agents_demo -s COMPLETED -c 5

```

## How to Extend

Each solver encapsulates one problem-solving approach. Use different LLM providers (GPT-4, Claude, Gemini) or prompt strategies for genuine diversity, add an impartial LLM judge, and the parallel-compete-evaluate workflow runs unchanged.

- **Solver workers** (`comp_solver_1/2/3`): use different LLM providers (GPT-4, Claude, Gemini) or different temperature/prompt configurations to produce genuinely diverse solutions
- **JudgeAgentWorker** (`comp_judge_agent`): use a separate LLM (ideally a different provider than the solvers) as the judge to avoid self-serving bias, with rubric-based scoring for objectivity
- **SelectWinnerWorker** (`comp_select_winner`): implement ensemble methods: instead of picking a single winner, merge the best elements from all three solutions using an LLM-based synthesis step

Swap in LLM calls for real solution generation; the parallel competition workflow preserves the same solve-judge-select interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
competitive-agents/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/competitiveagents/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CompetitiveAgentsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── JudgeAgentWorker.java
│       ├── SelectWinnerWorker.java
│       ├── Solver1Worker.java
│       ├── Solver2Worker.java
│       └── Solver3Worker.java
└── src/test/java/competitiveagents/workers/
    ├── JudgeAgentWorkerTest.java        # 9 tests
    ├── SelectWinnerWorkerTest.java        # 7 tests
    ├── Solver1WorkerTest.java        # 7 tests
    ├── Solver2WorkerTest.java        # 7 tests
    └── Solver3WorkerTest.java        # 7 tests

```
