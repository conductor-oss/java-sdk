# Autonomous Agent in Java Using Conductor: Goal-Directed Planning and Iterative Execution

You tell the agent "set up production monitoring for the platform." It provisions Grafana, wires up Prometheus, configures alerting rules. then deletes the test suite because a failing test was triggering alerts. Goal achieved, technically. The agent optimized for "no more alerts" instead of "working monitoring." Without a structured plan, progress checkpoints, and quality evaluation at each step, an autonomous agent will find the shortest path to its goal, and that path often goes through your guardrails instead of around them. This example decomposes a mission into a plan, executes steps in a Conductor `DO_WHILE` loop with progress evaluation at each iteration, and compiles a final report. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Autonomous Agents Need Plans, Not Just Loops

An agentic loop (think-act-observe) works for simple tasks, but complex missions like "Analyze our Q4 sales data and produce a competitive intelligence report" need a plan first. The agent should decompose the mission into concrete steps (gather sales data, identify top competitors, analyze market positioning, draft report sections, compile final report), then execute each step while tracking overall progress.

The plan provides structure, the agent knows how many steps remain, which step it's on, and what the end state looks like. After each execution step, a progress evaluation determines whether to continue (more steps to complete), retry (current step produced poor results), or finish (all steps done). Without orchestration, managing the plan state, tracking step completion across loop iterations, and handling execution failures mid-plan requires complex state management code.

## The Solution

**You write the goal-setting, planning, execution, and evaluation logic. Conductor handles plan state across iterations, step-level retries, and progress tracking.**

`SetGoalWorker` initializes the mission and success criteria. `CreatePlanWorker` decomposes the mission into ordered steps with descriptions and expected outputs. A `DO_WHILE` loop then iterates: `ExecuteStepWorker` runs the current plan step and returns results, and `EvaluateProgressWorker` checks whether the step succeeded, updates the completion percentage, and decides whether to continue. After the loop exits, `FinalReportWorker` compiles results from all executed steps into a comprehensive report. Conductor manages the plan state across iterations, retries failed execution steps without losing prior progress, and records each step's execution time and output.

### What You Write: Workers

Five workers implement the autonomous agent. Setting the goal, creating a plan, executing steps in a loop with progress evaluation, and compiling the final report.

| Worker | Task | What It Does |
|---|---|---|
| **CreatePlanWorker** | `aa_create_plan` | Creates a multi-step plan from a goal and its constraints. |
| **EvaluateProgressWorker** | `aa_evaluate_progress` | Evaluates progress after each executed step. |
| **ExecuteStepWorker** | `aa_execute_step` | Executes a single step from the plan based on the current iteration. |
| **FinalReportWorker** | `aa_final_report` | Produces the final report summarising the autonomous agent's work. |
| **SetGoalWorker** | `aa_set_goal` | Translates a high-level mission into a concrete goal with constraints. |

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
aa_set_goal
    │
    ▼
aa_create_plan
    │
    ▼
DO_WHILE
    └── aa_execute_step
    └── aa_evaluate_progress
    │
    ▼
aa_final_report
```

## Example Output

```
=== Autonomous Agent Demo ===

Step 1: Registering task definitions...
  Registered: aa_set_goal, aa_create_plan, aa_execute_step, aa_evaluate_progress, aa_final_report

Step 2: Registering workflow 'autonomous_agent'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: 6160fc78-0a29-4334-1318-0a76a49227e3

  [aa_set_goal] Mission: Set up production monitoring for the platform
  [aa_create_plan] Goal: Build and deploy a monitoring dashboard with alerting capabilities
  [aa_execute_step] Iteration 3: Success
  [aa_evaluate_progress] Iteration 3: assessment-value
  [aa_final_report] Mission '


  Status: COMPLETED
  Output: {mission=Set up production monitoring for the platform, goal=Build and deploy a monitoring dashboard with alerting capabilities, stepsExecuted=3, report=Mission ', success=true}

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
java -jar target/autonomous-agent-1.0.0.jar
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
java -jar target/autonomous-agent-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow autonomous_agent \
  --version 1 \
  --input '{"mission": "Set up production monitoring for the platform"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w autonomous_agent -s COMPLETED -c 5
```

## How to Extend

Each worker handles one phase of the autonomous mission. Use an LLM for dynamic plan generation, connect to real data tools (SQL, web scraping, APIs) for step execution, and add LLM-based quality evaluation, and the goal-plan-execute-evaluate workflow runs unchanged.

- **CreatePlanWorker** (`aa_create_plan`): use an LLM to generate dynamic plans from the mission description, with dependency tracking between steps and estimated completion times
- **ExecuteStepWorker** (`aa_execute_step`): connect to real tools based on the step type: SQL queries for data gathering, web scraping for competitive intelligence, LLM calls for analysis and writing
- **EvaluateProgressWorker** (`aa_evaluate_progress`): use an LLM to assess whether each step's output meets the plan's quality criteria, with the ability to request re-execution or plan adjustment

Wire in LLM reasoning for real planning and execution; the autonomous pipeline keeps the same goal-plan-execute-report contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):


## Project Structure

```
autonomous-agent/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/autonomousagent/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AutonomousAgentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreatePlanWorker.java
│       ├── EvaluateProgressWorker.java
│       ├── ExecuteStepWorker.java
│       ├── FinalReportWorker.java
│       └── SetGoalWorker.java
└── src/test/java/autonomousagent/workers/
    ├── CreatePlanWorkerTest.java        # 8 tests
    ├── EvaluateProgressWorkerTest.java        # 10 tests
    ├── ExecuteStepWorkerTest.java        # 9 tests
    ├── FinalReportWorkerTest.java        # 10 tests
    └── SetGoalWorkerTest.java        # 8 tests
```
