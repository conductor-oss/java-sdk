# Plan-Execute Agent in Java Using Conductor: Create Plan, Execute Three Steps, Compile Results

Tell an AI agent "deploy the new version" without a planning step and watch it start deploying immediately, realize halfway through it needs to run tests first, roll back, re-run tests, then deploy again: three times the work, three times the risk, because it never stopped to think before acting. The plan-execute pattern is the fix: create a structured plan upfront, execute each step in order, compile the results. This example uses [Conductor](https://github.com/conductor-oss/conductor) to separate planning from execution as distinct workers, if step 2 fails, Conductor retries it without re-running step 1 or regenerating the plan, and every intermediate result is persisted for inspection.

## Simple Objectives Need Simple Plans

Not every agent task needs an autonomous loop with goal evaluation at each step. "Research the top 3 JavaScript frameworks and produce a comparison" has a clear, fixed plan: (1) research React, (2) research Vue, (3) research Angular. Each step is independent, the plan doesn't change based on intermediate results, and the compilation step simply merges the three research outputs.

The plan-execute pattern is the simplest multi-step agent architecture: create a plan upfront, execute each step in order, compile the results. If step 2 fails (the Vue research source is down), retry it without re-running step 1 or re-creating the plan. The plan and all intermediate results are preserved across retries.

## The Solution

**You write the planning, step execution, and compilation logic. Conductor handles sequential chaining, per-step retries, and result assembly.**

`CreatePlanWorker` decomposes the objective into three ordered steps with descriptions and expected outputs. `ExecuteStep1Worker`, `ExecuteStep2Worker`, and `ExecuteStep3Worker` run each plan step sequentially, with each step receiving the plan context and prior step results. `CompileResultsWorker` merges all three step outputs into a final deliverable that addresses the original objective. Conductor chains these five steps and records each step's execution time and output for plan quality analysis.

### What You Write: Workers

Five workers implement plan-then-execute. Creating a structured plan, executing three steps in sequence, and compiling results into a final report.

| Worker | Task | What It Does |
|---|---|---|
| **CreatePlanWorker** | `pe_create_plan` | Decomposes a high-level objective into three ordered steps ("Gather market data and competitor analysis", "Analyze trends and identify opportunities", "Generate strategic recommendations"). Returns the step list and total step count. |
| **ExecuteStep1Worker** | `pe_execute_step_1` | Executes step 1 of the plan: gathers market data and competitor analysis. Returns a summary of 5 competitors analyzed and a $4.2B market size estimate. |
| **ExecuteStep2Worker** | `pe_execute_step_2` | Executes step 2: analyzes trends and identifies opportunities. Receives the step 1 result as context. Returns 3 growth opportunities (API platform, enterprise tier, international expansion). |
| **ExecuteStep3Worker** | `pe_execute_step_3` | Executes step 3: generates strategic recommendations. Receives the step 2 result as context. Returns prioritized recommendations with ROI estimates (API platform at capacity-planning%, enterprise tier at 210%). |
| **CompileResultsWorker** | `pe_compile_results` | Compiles all three step results into a single report string formatted as "Objective: .. | Step 1: .. | Step 2: .. | Step 3: ...". |

The simulated workers produce realistic, deterministic output shapes so the workflow runs end-to-end. To go to production, replace the simulation with the real API call, the worker interface stays the same, and no workflow changes are needed.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
pe_create_plan
    |
    v
pe_execute_step_1
    |
    v
pe_execute_step_2
    |
    v
pe_execute_step_3
    |
    v
pe_compile_results
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
java -jar target/plan-execute-agent-1.0.0.jar
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

## Example Output

```
=== Plan-Execute Agent Demo ===

Step 1: Registering task definitions...
  Registered: pe_create_plan, pe_execute_step_1, pe_execute_step_2, pe_execute_step_3, pe_compile_results

Step 2: Registering workflow 'plan_execute_agent'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  [pe_create_plan] Creating plan for objective: Develop go-to-market strategy for new SaaS product
  [pe_execute_step_1] Executing step 0: Gather market data and competitor analysis
  [pe_execute_step_2] Executing step 1: Analyze trends and identify opportunities
  [pe_execute_step_3] Executing step 2: Generate strategic recommendations
  [pe_compile_results] Compiling results for objective: Develop go-to-market strategy for new SaaS product

  Workflow ID: 3a7f8c12-e4b9-4d5a-b3c1-9f2d4e6a8b0c

Step 5: Waiting for completion...
  Status: COMPLETED
  Output: {objective=Develop go-to-market strategy for new SaaS product, plan=[Gather market data and competitor analysis, Analyze trends and identify opportunities, Generate strategic recommendations], finalReport=Objective: Develop go-to-market strategy for new SaaS product | Step 1: Collected data on 5 competitors; market size estimated at $4.2B | Step 2: Identified 3 growth opportunities: API platform, enterprise tier, international expansion | Step 3: Recommend prioritizing API platform (ROI: capacity-planning%) followed by enterprise tier (ROI: 210%)}

Result: PASSED
```

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/plan-execute-agent-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
# Plan a go-to-market strategy
conductor workflow start \
  --workflow plan_execute_agent \
  --version 1 \
  --input '{"objective": "Develop go-to-market strategy for new SaaS product"}'

# Plan a competitive analysis
conductor workflow start \
  --workflow plan_execute_agent \
  --version 1 \
  --input '{"objective": "Analyze competitive landscape for AI-powered customer support tools"}'

# Plan a product launch
conductor workflow start \
  --workflow plan_execute_agent \
  --version 1 \
  --input '{"objective": "Create launch plan for mobile app expansion into European markets"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w plan_execute_agent -s COMPLETED -c 5
```

## How to Extend

Each step worker is a self-contained executor. Connect an LLM for dynamic plan creation, SerpAPI or databases for research steps, and an LLM for narrative report compilation, and the plan-execute-compile workflow runs unchanged.

- **CreatePlanWorker** (`pe_create_plan`): use OpenAI function calling or Anthropic tool use to generate dynamic plans with variable step counts based on objective complexity, or integrate LangChain's `PlanAndExecute` agent to decompose objectives into dependency-aware task graphs
- **Execute workers** (`pe_execute_step_1/2/3`): connect to real tools based on step type: SerpAPI or Tavily for web research, Pandas/DuckDB for data analysis, or call domain-specific APIs (Crunchbase for competitor data, Statista for market sizing)
- **CompileResultsWorker** (`pe_compile_results`): use GPT-4 or Claude to synthesize step results into a coherent narrative report with executive summary, comparison tables, and prioritized recommendations
- **Add more steps**: create additional `ExecuteStepNWorker` classes and extend the workflow JSON with new tasks. Each step worker follows the same interface, so adding steps is copy-paste.

Replace with LLM-generated plans and real data queries; the plan-execute pipeline preserves the same step-by-step interface.

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
plan-execute-agent/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/planexecuteagent/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PlanExecuteAgentExample.java # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CreatePlanWorker.java     # Decomposes objective into 3 ordered steps
│       ├── ExecuteStep1Worker.java   # Market data and competitor analysis
│       ├── ExecuteStep2Worker.java   # Trend analysis and opportunity identification
│       ├── ExecuteStep3Worker.java   # Strategic recommendations with ROI estimates
│       └── CompileResultsWorker.java # Merges step results into final report
└── src/test/java/planexecuteagent/workers/
    ├── CreatePlanWorkerTest.java     # 9 tests
    ├── ExecuteStep1WorkerTest.java   # 9 tests
    ├── ExecuteStep2WorkerTest.java   # 9 tests
    ├── ExecuteStep3WorkerTest.java   # 9 tests
    └── CompileResultsWorkerTest.java # 9 tests
```
