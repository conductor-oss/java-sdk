# Reflection Agent in Java Using Conductor :  Generate, Reflect, Improve in Iterative Refinement Loop

Reflection Agent .  generates content on a topic, iteratively reflects and improves through a DO_WHILE loop, then produces final polished output. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## First Drafts Are Never Good Enough

An LLM's first response to "Write an essay about climate change solutions" is serviceable but rarely excellent. It might lack specific examples, have a weak conclusion, or miss an important perspective. A reflection agent catches these weaknesses: after generating the initial draft, a separate reflection step identifies specific issues ("Paragraph 3 lacks a concrete example", "The economic analysis is superficial"), and an improvement step addresses each issue.

Each iteration sharpens the output .  the first reflection might catch structural problems, the second might catch factual gaps, the third might polish prose quality. The loop terminates when the reflection step finds no significant issues or a maximum iteration count is reached. Without orchestration, managing the evolving draft across iterations, tracking which issues were found and fixed, and implementing loop termination logic requires careful state management.

## The Solution

**You write the generation, reflection, and improvement logic. Conductor handles the refinement loop, quality threshold evaluation, and version tracking.**

`InitialGenerationWorker` produces the first draft of the content based on the topic. A `DO_WHILE` loop then iterates: `ReflectWorker` analyzes the current version and identifies specific weaknesses with severity ratings and suggested improvements. `ImproveWorker` addresses each identified weakness, producing an improved version with change annotations. After the loop exits (quality threshold met or max iterations reached), `FinalOutputWorker` delivers the polished content with a summary of all reflections and improvements made. Conductor tracks each iteration's reflection and improvement for quality analysis.

### What You Write: Workers

Four workers power iterative refinement. Generating an initial draft, reflecting on weaknesses, improving based on feedback, and delivering the polished output.

| Worker | Task | What It Does |
|---|---|---|
| **FinalOutputWorker** | `rn_final_output` | Produces the final polished output after all reflection iterations. Combines the improved drafts into a high-quality ... |
| **ImproveWorker** | `rn_improve` | Incorporates reflection feedback to improve the draft. Returns the revised content and a flag indicating the feedback... |
| **InitialGenerationWorker** | `rn_initial_generation` | Generates an initial draft on a given topic. Produces a shallow first pass with a low quality score, setting the stag... |
| **ReflectWorker** | `rn_reflect` | Reflects on the current draft and provides constructive feedback along with a quality score. The feedback varies by i... |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode .  the agent workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Loop execution** | DO_WHILE repeats a set of tasks until a condition is met |

### The Workflow

```
rn_initial_generation
    │
    ▼
DO_WHILE
    └── rn_reflect
    └── rn_improve
    │
    ▼
rn_final_output
```

## Example Output

```
=== Reflection Agent Demo ===

Step 1: Registering task definitions...
  Registered: rn_initial_generation, rn_reflect, rn_improve, rn_final_output

Step 2: Registering workflow 'reflection_agent'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [rn_final_output] Producing final output on:
  [rn_improve] Improving draft (iteration
  [rn_initial_generation] Generating initial draft on:
  [rn_reflect] Reflecting on draft (iteration

  Status: COMPLETED
  Output: {content=..., qualityScore=..., applied=..., feedback=...}

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
java -jar target/reflection-agent-1.0.0.jar
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
java -jar target/reflection-agent-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow reflection_agent \
  --version 1 \
  --input '{"topic": "sample-topic"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w reflection_agent -s COMPLETED -c 5
```

## How to Extend

Each worker handles one phase of the iterative refinement cycle. Use an LLM for initial generation, a separate LLM (or different provider) for critical reflection, and targeted improvement prompts for revision, and the generate-reflect-improve loop runs unchanged.

- **ReflectWorker** (`rn_reflect`): use a different LLM than the generator as the reflector (e.g., Claude reflects on GPT-4's output) to avoid self-serving bias in quality assessment
- **ImproveWorker** (`rn_improve`): use structured feedback: the reflection produces specific, actionable items (add example to paragraph 3, expand economic analysis) that the improver addresses one by one
- **InitialGenerationWorker** (`rn_initial_generation`): use GPT-4 with domain-specific system prompts and reference material for higher-quality initial drafts that require fewer reflection iterations

Plug in real LLM generation and critique; the reflection loop maintains the same generate-reflect-improve interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):


## Project Structure

```
reflection-agent/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/reflectionagent/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ReflectionAgentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FinalOutputWorker.java
│       ├── ImproveWorker.java
│       ├── InitialGenerationWorker.java
│       └── ReflectWorker.java
└── src/test/java/reflectionagent/workers/
    ├── FinalOutputWorkerTest.java        # 9 tests
    ├── ImproveWorkerTest.java        # 8 tests
    ├── InitialGenerationWorkerTest.java        # 8 tests
    └── ReflectWorkerTest.java        # 9 tests
```
