# ReAct Agent in Java Using Conductor -- Reason-Act-Observe Loop for Question Answering

Someone asks "What's the GDP per capita of the country that hosted the 2024 Olympics?" A standard LLM either guesses or admits it doesn't know. A naive agent fires off a search, gets a wall of text about the Paris ceremony, and tries to answer from that alone -- never connecting the dots to look up France's GDP. It acted before it thought, and now it's reasoning backwards from incomplete data it already committed to. The ReAct pattern fixes this: reason first (what do I need to know?), then act (search for it), then observe (did that answer my question or do I need another step?). This example builds that loop with Conductor's `DO_WHILE`. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers -- you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Questions That Require Multi-Step Reasoning and Action

"What is the GDP per capita of the country that hosted the 2024 Olympics?" can't be answered in one step. The agent needs to reason ("I need to find which country hosted the 2024 Olympics"), act (search for "2024 Olympics host country"), observe the result ("France"), reason again ("Now I need France's GDP per capita"), act again (search for "France GDP per capita"), and observe ("$44,408"). Only then can it produce the final answer.

The ReAct pattern interleaves reasoning (what should I do next?) with acting (executing the chosen action) and observing (interpreting the result). The agent decides when it has enough information to stop -- making this an inherently iterative process. Without orchestration, the reasoning-acting-observing loop with proper state management, action retry logic, and loop termination conditions requires complex stateful code.

## The Solution

**You write the reasoning, action execution, and observation logic. Conductor handles the ReAct loop, context accumulation, and termination control.**

`InitTaskWorker` sets up the question and initializes the reasoning context. A `DO_WHILE` loop then iterates: `ReasonWorker` examines the question and accumulated observations to decide the next action (search, calculate, or answer). `ActWorker` executes the chosen action -- web search, calculation, database lookup -- and returns raw results. `ObserveWorker` interprets the action results, updates the context with new information, and determines whether the agent has enough information to answer. After the loop exits, `FinalAnswerWorker` produces the answer using all accumulated observations. Conductor manages the growing context across iterations and records every reasoning-action-observation triple.

### What You Write: Workers

Five workers drive the ReAct loop -- initializing the question, then iterating through reasoning, action execution, and observation until the answer is found.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **ActWorker** | `rx_act` | Executes the action decided by the ReasonWorker. If the action is "search", returns a deterministic search result. Ot... | Simulated |
| **FinalAnswerWorker** | `rx_final_answer` | Produces the final answer after all ReAct iterations are complete. Returns a deterministic answer with a confidence s... | Simulated |
| **InitTaskWorker** | `rx_init_task` | Initializes the ReAct agent loop by capturing the question and creating an empty context list for subsequent iterations. | Simulated |
| **ObserveWorker** | `rx_observe` | Observes the result of the action and determines whether the information is useful for answering the question. Always... | Simulated |
| **ReasonWorker** | `rx_reason` | Reasons about the question and decides what action to take next. Produces a thought, an action type, and a query for ... | Simulated |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode -- the agent workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically -- configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status -- no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Loop execution** | DO_WHILE repeats a set of tasks until a condition is met |

### The Workflow

```
rx_init_task
    │
    ▼
DO_WHILE
    └── rx_reason
    └── rx_act
    └── rx_observe
    │
    ▼
rx_final_answer
```

## Example Output

```
=== ReAct Agent Demo ===

Step 1: Registering task definitions...
  Registered: rx_init_task, rx_reason, rx_act, rx_observe, rx_final_answer

Step 2: Registering workflow 'react_agent'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: b76d7575-fe60-fa8b-3c72-e4c50ebe00d6

  [rx_init_task] Initializing ReAct agent for: What is the current world population?
  [rx_reason] Iteration 3: thought-value
  [rx_act] Action: proceed
  [rx_observe] Iteration 
  [rx_final_answer] Generating final answer after 3 iterations


  Status: COMPLETED
  Output: {question=What is the current world population?, iterations=3, answer=The world population is approximately 8.1 billion people as of 2024, confirmed by multiple sources.}

Result: PASSED
```
## Running It

### Prerequisites

- **Java 21+** -- verify with `java -version`
- **Maven 3.8+** -- verify with `mvn -version`
- **Docker** -- to run Conductor

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
java -jar target/react-agent-1.0.0.jar
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
java -jar target/react-agent-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow react_agent \
  --version 1 \
  --input '{"question": "What is the current world population?"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w react_agent -s COMPLETED -c 5
```

## How to Extend

Each worker handles one phase of the Reason-Act-Observe cycle -- use an LLM with ReAct prompting for reasoning, connect real tools (SerpAPI, Wolfram Alpha, Wikipedia) for actions, and add LLM-based observation evaluation, and the iterative reasoning loop runs unchanged.

- **ReasonWorker** (`rx_reason`) -- use GPT-4 or Claude with the ReAct prompt format (Thought/Action/Observation) for genuine multi-step reasoning with tool selection
- **ActWorker** (`rx_act`) -- connect to real tools: SerpAPI or Tavily for web search, Wolfram Alpha for calculations, Wikipedia API for factual lookups, and SQL databases for data queries
- **ObserveWorker** (`rx_observe`) -- use an LLM to extract relevant facts from action results, assess confidence levels, and determine whether the question can be answered or more actions are needed

Wire in real search APIs and LLM reasoning; the reason-act-observe loop uses the same iteration contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):


## Project Structure

```
react-agent/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/reactagent/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ReactAgentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ActWorker.java
│       ├── FinalAnswerWorker.java
│       ├── InitTaskWorker.java
│       ├── ObserveWorker.java
│       └── ReasonWorker.java
└── src/test/java/reactagent/workers/
    ├── ActWorkerTest.java        # 9 tests
    ├── FinalAnswerWorkerTest.java        # 8 tests
    ├── InitTaskWorkerTest.java        # 8 tests
    ├── ObserveWorkerTest.java        # 8 tests
    └── ReasonWorkerTest.java        # 9 tests
```
