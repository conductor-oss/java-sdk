# Agent Supervisor in Java Using Conductor: Plan, Fan Out to Coder/Tester/Documenter, Review

Assign code review to three unsupervised AI agents and watch what happens: one rewrites your architecture unprompted, another nitpicks formatting while ignoring logic bugs, and the third hallucinates a critical security vulnerability that doesn't exist. Without a supervisor to define the plan upfront and gate the output afterward, parallel agents produce inconsistent, uncoordinated work that nobody catches until it ships. This example uses [Conductor](https://github.com/conductor-oss/conductor) to implement the supervisor pattern, a planning worker defines the scope, `FORK_JOIN` fans out to coder, tester, and documenter agents in parallel, and a review worker inspects all outputs against the original plan before anything is delivered.

## Coordinating Specialist Agents Needs a Supervisor

Building a feature requires coding, testing, and documentation. three tasks that can run in parallel but all depend on the same plan and all need quality review afterward. The coder implements the feature (AuthController, AuthService, AuthRepository, 245 lines of Java with JWT handling). The tester writes and runs test cases (unit tests, integration tests, edge cases, 18 tests, 82% coverage). The documenter creates API docs, architecture diagrams, and a changelog.

Without a supervisor, these agents work independently with no shared plan and no quality gate. The coder might implement something the tester doesn't cover. The documenter might describe an API that doesn't match the implementation. The supervisor pattern adds two coordination steps: planning before execution (so all agents work from the same specification) and review after execution (so inconsistencies are caught before delivery).

## The Solution

**You write the planning, agent, and review logic. Conductor handles parallel fan-out, synchronization, and quality tracking.**

`PlanWorker` (the supervisor) creates a development plan with feature requirements, tasks for each specialist, and acceptance criteria. `FORK_JOIN` fans out to three agents in parallel: `CoderAgentWorker` implements the feature and reports files created and lines of code, `TesterAgentWorker` writes and runs tests reporting coverage and pass rates, and `DocumenterAgentWorker` generates API docs and architecture documentation. After `JOIN` synchronizes all three, `ReviewWorker` (the supervisor again) inspects all outputs against the plan, flags inconsistencies, and produces a quality report with per-agent scores. Conductor handles the parallel execution, waits for all agents to finish, and tracks each agent's execution time and output.

### What You Write: Workers

A supervisor plans the work, three specialist agents (coder, tester, documenter) execute in parallel, and the supervisor reviews all outputs for quality.

| Worker | Task | What It Does |
|---|---|---|
| **PlanWorker** | `sup_plan` | Creates a development plan for the requested feature. Returns a plan object with feature name, priority, 5 development phases (design, implementation, testing, documentation, review), a deadline, and task descriptions for each specialist agent. |
| **CoderAgentWorker** | `sup_coder_agent` | Implements the feature. Returns implementation results: 3 files created (AuthController, AuthService, AuthRepository), 245 lines of Java, and implementation notes describing JWT token handling and role-based access control. |
| **TesterAgentWorker** | `sup_tester_agent` | Creates and runs test suites. Returns test execution results: 3 test suites, 18 total tests, 17 passed, 1 failed (testTokenExpirationEdgeCase), 82% coverage. Status is "needs_fix" due to the failing test. |
| **DocumenterAgentWorker** | `sup_documenter_agent` | Generates documentation. Returns 3 documents created (API_REFERENCE.md, SETUP_GUIDE.md, EXAMPLES.md), 5 sections (Overview, Authentication Flow, API Endpoints, Configuration, Troubleshooting), and 1200 words total. |
| **ReviewWorker** | `sup_review` | Reviews all agent outputs against the plan. Determines overall status: APPROVED if all agents pass, NEEDS_REVISION if any agent has issues. Returns action items (fix failing test, increase coverage, add error handling docs) and metrics (lines of code, test count, documentation word count). |

The simulated workers produce realistic, deterministic output shapes so the workflow runs end-to-end. To go to production, replace the simulation with the real API call, the worker interface stays the same, and no workflow changes are needed.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Parallel execution** | `FORK_JOIN` runs coder, tester, and documenter simultaneously and waits for all to complete |
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
sup_plan
    |
    v
FORK_JOIN
    +-- sup_coder_agent
    +-- sup_tester_agent
    +-- sup_documenter_agent
    |
    v
JOIN (wait for all branches)
    |
    v
sup_review
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
java -jar target/agent-supervisor-1.0.0.jar
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
=== Example 168: Agent Supervisor ===

Step 1: Registering task definitions...
  Registered: sup_plan, sup_coder_agent, sup_tester_agent, sup_documenter_agent, sup_review

Step 2: Registering workflow 'agent_supervisor'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  [plan] Created plan for feature 'user-authentication' with priority 'high'
  [coder] Completed coding for 'user-authentication': 3 files, 245 lines
  [tester] Completed testing for 'user-authentication': 17/18 passed, 82% coverage
  [documenter] Completed documentation for 'user-authentication': 3 documents, 1200 words
  [review] Review complete for 'user-authentication': NEEDS_REVISION

  Workflow ID: 7b2c3d4e-5f6a-7890-abcd-ef0123456789

Step 5: Waiting for completion...
  Status: COMPLETED
  Feature: user-authentication
  Overall status: NEEDS_REVISION
  Action items: [Fix failing test: testTokenExpirationEdgeCase, Increase test coverage from 82% to at least 90%, Add error handling documentation section]
  Agents used: [coder, tester, documenter]

--- Agent Supervisor Pattern ---
  - Plan: Create development plan with task assignments
  - FORK: Coder, tester, and documenter agents work in parallel
  - JOIN: Collect all agent results
  - Review: Supervisor reviews and produces final report

Result: PASSED
```

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/agent-supervisor-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
# Build the user-authentication feature
conductor workflow start \
  --workflow agent_supervisor \
  --version 1 \
  --input '{"feature": "user-authentication", "priority": "high", "systemPrompt": "You are a senior engineering supervisor coordinating agent work."}'

# Build a payment-processing feature
conductor workflow start \
  --workflow agent_supervisor \
  --version 1 \
  --input '{"feature": "payment-processing", "priority": "critical", "systemPrompt": "You are a senior engineering supervisor coordinating agent work."}'

# Build a search feature with normal priority
conductor workflow start \
  --workflow agent_supervisor \
  --version 1 \
  --input '{"feature": "full-text-search", "priority": "medium", "systemPrompt": "You are a senior engineering supervisor coordinating agent work."}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w agent_supervisor -s COMPLETED -c 5
```

## How to Extend

Each specialist agent is self-contained. Connect the coder to GitHub/Codex, the tester to Maven Surefire/JaCoCo, and the documenter to an LLM docs pipeline, and the plan-fork-review supervisory workflow runs unchanged.

- **PlanWorker** (`sup_plan`): use GPT-4 or Claude to generate context-aware development plans from feature specifications, or integrate with Jira/Linear APIs to create real tasks with story points and sprint assignments
- **CoderAgentWorker** (`sup_coder_agent`): integrate with GitHub's API to create branches and commit code, or use OpenAI Codex / Anthropic tool use to generate real implementations from the supervisor's task specification
- **TesterAgentWorker** (`sup_tester_agent`): execute real test suites via Maven Surefire or JUnit Platform Launcher, parse JaCoCo coverage reports, and return actual pass/fail metrics with failing test details
- **DocumenterAgentWorker** (`sup_documenter_agent`): use an LLM to generate API documentation from the coder's output, or integrate with Swagger/OpenAPI to auto-generate reference docs from code annotations
- **ReviewWorker** (`sup_review`): use an LLM to cross-reference the coder's implementation against the tester's coverage and the documenter's API descriptions, flagging gaps automatically
- **Add a new agent**: create a new worker class, add a branch to the `FORK_JOIN` in `workflow.json`, and update the `JOIN` and `ReviewWorker` inputs. No existing code changes needed.

Replace with real code generation and test frameworks; the supervisor pipeline maintains the same plan-execute-review contract.

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
agent-supervisor/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/agentsupervisor/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AgentSupervisorExample.java  # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PlanWorker.java          # Creates dev plan with phases and task assignments
│       ├── CoderAgentWorker.java    # Implements feature, reports files and LOC
│       ├── TesterAgentWorker.java   # Runs tests, reports coverage and pass rates
│       ├── DocumenterAgentWorker.java # Generates docs, reports sections and word count
│       └── ReviewWorker.java        # Reviews all outputs, produces quality report
└── src/test/java/agentsupervisor/workers/
    ├── PlanWorkerTest.java          # 11 tests
    ├── CoderAgentWorkerTest.java    # 10 tests
    ├── TesterAgentWorkerTest.java   # 10 tests
    ├── DocumenterAgentWorkerTest.java # 9 tests
    └── ReviewWorkerTest.java        # 12 tests
```
