# Agent Collaboration in Java Using Conductor: Sequential Analyst, Strategist, Executor, and Plan Compiler

Four specialized AI agents chained in sequence sounds clean on a whiteboard, but in practice Agent 2 sits idle while Agent 1 thinks, Agent 3 has no way to flag that Agent 2's output is garbage, and when Agent 1's LLM call times out you end up re-running the entire pipeline because nothing preserved the intermediate state. This example uses [Conductor](https://github.com/conductor-oss/conductor) to wire an analyst, strategist, executor, and plan compiler into a durable sequential pipeline, each agent is an independent worker with its own retries and timeout, Conductor persists every intermediate output, and you can inspect exactly which agent produced which piece of the final business plan.

## Business Planning Requires Multiple Perspectives in Sequence

A useful business plan requires analysis, strategy, execution planning, and synthesis. Four distinct cognitive tasks that build on each other. The analyst identifies problems (customer churn concentrated in the 90-day post-acquisition window, repeat purchase rate declining 22% QoQ). The strategist takes those findings and develops responses (retention program, pricing restructure). The executor turns strategies into implementation tasks with deadlines and resource requirements. The compiler weaves all three outputs into a coherent plan.

Each agent's output is the next agent's input, the strategist can't develop recommendations without the analyst's findings, and the executor can't create tasks without the strategist's priorities. If the strategist's LLM call times out, you need to retry it with the analyst's findings still available, not re-run the entire analysis. And you need to see each agent's individual contribution to understand how the final plan was assembled.

## The Solution

**You write each agent's analysis and planning logic. Conductor handles sequencing, data passing between agents, and durability.**

`AnalystWorker` examines the business context and produces structured findings with severity ratings (critical, high, medium) and a metrics summary. `StrategistWorker` takes those findings and develops strategic recommendations with priority rankings and expected impact. `ExecutorWorker` converts the strategy into concrete implementation tasks with owners, timelines, and resource estimates. `CompilePlanWorker` assembles the analysis, strategy, and execution plan into a final deliverable. Conductor sequences them strictly, retries any failed agent call, and records each agent's output so you can trace how every recommendation in the final plan connects back to a specific finding.

### What You Write: Workers

Four agents collaborate sequentially, the analyst produces findings, the strategist develops recommendations, the executor creates tasks, and the compiler assembles the plan.

| Worker | Task | What It Does |
|---|---|---|
| **AnalystWorker** | `ac_analyst` | Examines the business context and produces 4 structured insights with severity ratings (critical, high, medium) across retention, revenue, operations, and engagement categories. Returns a metrics summary with churn rate (15%), repeat purchase decline (22%), support response time (48h), and loyalty engagement (12%). |
| **StrategistWorker** | `ac_strategist` | Takes the analyst's insights and metrics, then formulates the "Stabilize & Retain" strategy with a thesis, 3 strategic pillars (Customer Experience Overhaul, Support Response Acceleration, Loyalty Program Revitalization), and 4 ranked priorities with effort/impact assessments. |
| **ExecutorWorker** | `ac_executor` | Translates the strategy into 6 concrete action items with owners (Marketing, Customer Support, Product, Data Science, Operations), deadlines (Week 2-8), and priority levels. Creates a 3-phase, 8-week timeline (Quick Wins, Foundation, Scale & Sustain). |
| **CompilePlanWorker** | `ac_compile_plan` | Assembles insights, strategy, action items, and timeline into a consolidated plan summary. Reports counts (insights used, strategy pillars, action items) and timeline duration. Sets status to "ready_for_review". |

The simulated workers produce realistic, deterministic output shapes so the workflow runs end-to-end. To go to production, replace the simulation with the real API call, the worker interface stays the same, and no workflow changes are needed.

### The Workflow

```
ac_analyst
    |
    v
ac_strategist
    |
    v
ac_executor
    |
    v
ac_compile_plan

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
java -jar target/agent-collaboration-1.0.0.jar

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
java -jar target/agent-collaboration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
# E-commerce churn analysis
conductor workflow start \
  --workflow agent_collaboration_demo \
  --version 1 \
  --input '{"businessContext": "E-commerce platform experiencing 15% customer churn rate with declining repeat purchases"}'

# SaaS growth planning
conductor workflow start \
  --workflow agent_collaboration_demo \
  --version 1 \
  --input '{"businessContext": "B2B SaaS company with strong acquisition but 40% annual churn and declining NPS scores"}'

# Market expansion strategy
conductor workflow start \
  --workflow agent_collaboration_demo \
  --version 1 \
  --input '{"businessContext": "Mid-market fintech looking to expand into European markets while facing increasing regulatory pressure"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w agent_collaboration_demo -s COMPLETED -c 5

```

## How to Extend

Each agent in the pipeline owns one cognitive role. Plug in real data sources (Mixpanel, QuickBooks), LLMs for strategy generation, and project management APIs (Jira, Linear), and the four-stage business planning workflow runs unchanged.

- **AnalystWorker** (`ac_analyst`): connect to real data sources: pull metrics from Mixpanel/Amplitude for user behavior analysis, query financial data from QuickBooks/Xero APIs, or scrape competitor data from SimilarWeb
- **StrategistWorker** (`ac_strategist`): use GPT-4 or Claude with domain-specific system prompts to generate strategic recommendations, or integrate with strategy frameworks like SWOT/Porter's Five Forces templates
- **ExecutorWorker** (`ac_executor`): create real tasks in Jira/Linear/Asana via their APIs, assign to team members, set sprint deadlines, and link to the strategy document for traceability
- **CompilePlanWorker** (`ac_compile_plan`): generate the final plan as a formatted PDF using Apache PDFBox, push it to Google Docs via the Docs API, or post a summary to Slack/Teams for stakeholder review
- **Add a new agent**: create a new worker class and insert it into the workflow JSON sequence. For example, add a `BudgetWorker` between executor and compiler to estimate costs for each action item.

Swap in LLM calls or analytics APIs for real analysis; the four-agent pipeline keeps the same data handoff between steps.

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
agent-collaboration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/agentcollaboration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AgentCollaborationExample.java # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalystWorker.java       # 4 insights with severity ratings and metrics
│       ├── StrategistWorker.java    # Strategy with thesis, pillars, ranked priorities
│       ├── ExecutorWorker.java      # 6 action items with owners and 8-week timeline
│       └── CompilePlanWorker.java   # Assembles plan summary with counts and status
└── src/test/java/agentcollaboration/workers/
    ├── AnalystWorkerTest.java       # 9 tests
    ├── StrategistWorkerTest.java    # 10 tests
    ├── ExecutorWorkerTest.java      # 8 tests
    └── CompilePlanWorkerTest.java   # 9 tests

```
