# Agentic Loop in Java Using Conductor: Think-Act-Observe Iteration Until Goal Completion

You tell the agent "research distributed consensus algorithms." It searches, finds three papers, and searches again. And again. And again. Forty minutes and $50 in API calls later, it's still searching because nobody told it when to stop. The agent has no concept of "done".; no iteration cap, no goal-completion check, no kill switch. This example builds a think-act-observe loop with Conductor's `DO_WHILE` that gives the agent autonomy within guardrails: it reasons about what to do next, executes the plan, evaluates the result, and terminates when the goal is met or the iteration limit is hit. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers. You write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Complex Goals Require Iterative Reasoning

Some tasks can't be solved in a single step. "Research distributed consensus algorithms and produce recommendations" requires gathering information, analyzing patterns, and synthesizing findings, and the agent might need multiple cycles to converge. In the first iteration, it gathers sources. In the second, it identifies patterns (consistency, partition tolerance, replication strategies, consensus protocols, failure recovery). In the third, it synthesizes recommendations.

The think-act-observe loop gives the agent autonomy within guardrails: it decides what to do next (think), executes that plan (act), evaluates the result (observe), and decides whether the goal is met or another iteration is needed. The loop must terminate. Either the agent achieves the goal or hits a maximum iteration count. Without orchestration, implementing this loop with proper state management between iterations, retry logic for failed actions, and observability into each iteration's reasoning is error-prone.

## The Solution

**You write the thinking, acting, and observing logic. Conductor handles the iteration loop, state persistence, and termination control.**

`SetGoalWorker` initializes the agent's mission and success criteria. A `DO_WHILE` loop then iterates: `ThinkWorker` examines the current state and plans the next action ("Research and gather information", then "Analyze gathered data", then "Synthesize findings"). `ActWorker` executes the planned action and returns results. `ObserveWorker` evaluates the results, updates progress tracking, and determines whether the goal is met. After the loop exits, `SummarizeWorker` compiles the findings from all iterations into a final report. Conductor manages the loop state between iterations, retries failed actions without losing prior iteration results, and records every think-act-observe cycle for debugging.

### What You Write: Workers

Five workers drive the iterative loop. Setting the goal, then cycling through think-act-observe until completion, and summarizing the findings.

| Worker | Task | What It Does |
|---|---|---|
| **ActWorker** | `al_act` | Executes the action described by the plan and returns a result. Results are deterministically mapped from plan strings. |
| **ObserveWorker** | `al_observe` | Observes the outcome of the action and assesses goal progress. |
| **SetGoalWorker** | `al_set_goal` | Initializes the agentic loop by accepting a goal and marking it as active. |
| **SummarizeWorker** | `al_summarize` | Summarizes the agentic loop execution after all iterations are complete. |
| **ThinkWorker** | `al_think` | Plans the next action based on the goal and current iteration. Cycles through 3 fixed plan strings based on iteration |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode, the agent workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Loop execution** | DO_WHILE repeats a set of tasks until a condition is met |

### The Workflow

```
al_set_goal
    │
    ▼
DO_WHILE
    └── al_think
    └── al_act
    └── al_observe
    │
    ▼
al_summarize
```

## Example Output

```
=== Agentic Loop Demo ===

Step 1: Registering task definitions...
  Registered: al_set_goal, al_think, al_act, al_observe, al_summarize

Step 2: Registering workflow 'agentic_loop'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: 865926d6-00fc-a6cb-6ccf-2e85a289bb51

  [al_set_goal] Setting goal: Research best practices for distributed systems
  [al_think] Iteration 3: Execute all steps
  [al_act] Iteration 3: proceed
  [al_observe] Iteration 3: 2026-03-16
  [al_summarize] Achieved goal '


  Status: COMPLETED
  Output: {goal=Research best practices for distributed systems, iterations=3, summary=Achieved goal '}

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
java -jar target/agentic-loop-1.0.0.jar
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
java -jar target/agentic-loop-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow agentic_loop \
  --version 1 \
  --input '{"goal": "Research best practices for distributed systems"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w agentic_loop -s COMPLETED -c 5
```

## How to Extend

Each worker handles one phase of the think-act-observe cycle. Plug in a real LLM for planning, connect to web search or database APIs for actions, and use an LLM evaluator for observations, and the iterative loop workflow runs unchanged.

- **ThinkWorker** (`al_think`): use GPT-4 or Claude to generate action plans based on the current state, prior observations, and remaining goals. Enabling genuine autonomous reasoning instead of scripted plans
- **ActWorker** (`al_act`): connect to real tools: web search APIs for research, code execution sandboxes for computation, database queries for data gathering, or HTTP calls to external services
- **ObserveWorker** (`al_observe`): use an LLM to evaluate action results against the original goal, with structured output indicating completion percentage, remaining gaps, and whether to continue iterating

Plug in real LLM reasoning and tool execution; the think-act-observe loop uses the same iteration interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):


## Project Structure

```
agentic-loop/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/agenticloop/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AgenticLoopExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ActWorker.java
│       ├── ObserveWorker.java
│       ├── SetGoalWorker.java
│       ├── SummarizeWorker.java
│       └── ThinkWorker.java
└── src/test/java/agenticloop/workers/
    ├── ActWorkerTest.java        # 9 tests
    ├── ObserveWorkerTest.java        # 9 tests
    ├── SetGoalWorkerTest.java        # 8 tests
    ├── SummarizeWorkerTest.java        # 8 tests
    └── ThinkWorkerTest.java        # 9 tests
```
