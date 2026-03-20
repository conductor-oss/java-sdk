# Two-Agent Pipeline in Java Using Conductor :  Writer Agent to Editor Agent

Sequential writer-editor pipeline: writer agent drafts content, editor agent refines it, final output assembles the result. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Two Agents Are Better Than One

A single LLM call to "write a product description" produces serviceable but unpolished output. Adding a second pass .  where a different agent reviews and improves the first agent's output ,  consistently produces better results. The writer focuses on content and structure. The editor focuses on clarity, conciseness, and polish. Two specialized agents with a single handoff.

This is the most common multi-agent pattern: agent A produces output, agent B improves it. It works for content (writer + editor), code (generator + reviewer), analysis (analyst + critic), and many other domains. Separating generation from refinement means you can swap either agent independently, compare different writer/editor combinations, and see exactly what the editor changed.

## The Solution

**You write the content generation and editorial refinement logic. Conductor handles the handoff, version tracking, and quality comparison between drafts.**

`WriterAgentWorker` generates the initial content .  a product description, blog post, or marketing copy ,  based on the input parameters. `EditorAgentWorker` reviews the writer's output and improves it ,  fixing grammar, improving clarity, tightening prose, and ensuring the content meets style guidelines. `FinalOutputWorker` packages the edited content with metadata (word count, readability score, edit summary). Conductor chains the three steps and records both the original and edited versions for quality comparison.

### What You Write: Workers

Three workers form the simplest multi-agent pattern, the writer drafts content, the editor refines it, and the final output assembles the result with metadata.

| Worker | Task | What It Does |
|---|---|---|
| **EditorAgentWorker** | `tap_editor_agent` | Editor agent that reviews and improves a draft. Takes draft, tone, and systemPrompt as inputs and returns editedConte... |
| **FinalOutputWorker** | `tap_final_output` | Final output worker that assembles the pipeline result. Takes originalDraft, editedContent, and editorNotes as inputs... |
| **WriterAgentWorker** | `tap_writer_agent` | Writer agent that produces a draft about a given topic. Takes topic, tone, and systemPrompt as inputs and returns a d... |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode .  the agent workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
tap_writer_agent
    │
    ▼
tap_editor_agent
    │
    ▼
tap_final_output
```

## Example Output

```
=== Example 166: Two-Agent Pipeline. Writer + Editor ===

Step 1: Registering task definitions...
  Registered: tap_writer_agent, tap_editor_agent, tap_final_output

Step 2: Registering workflow 'two_agent_pipeline'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [editor-agent] Edited draft with 4 changes, tone:
  [final-output] Assembled pipeline result: original=
  [writer-agent] Drafted content about '

  Status: COMPLETED
  Output: {editedContent=..., notes=..., changesCount=..., model=...}

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
java -jar target/two-agent-pipeline-1.0.0.jar
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
java -jar target/two-agent-pipeline-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow two_agent_pipeline \
  --version 1 \
  --input '{}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w two_agent_pipeline -s COMPLETED -c 5
```

## How to Extend

Each agent owns one side of the generate-then-refine pattern. Use an LLM for initial content drafting and a different LLM (or provider) for editorial polish, and the writer-editor pipeline runs unchanged.

- **WriterAgentWorker** (`tap_writer_agent`): use GPT-4 or Claude with domain-specific system prompts (product descriptions, technical writing, marketing copy) for higher-quality initial drafts
- **EditorAgentWorker** (`tap_editor_agent`): use a different LLM than the writer to avoid self-confirmation bias, with editing-specific prompts focusing on grammar, style, and factual accuracy
- **FinalOutputWorker** (`tap_final_output`): compute readability metrics (Flesch-Kincaid, Coleman-Liau), diff the writer and editor versions to quantify editing impact, and route to human review if changes exceed a threshold

Swap in LLM calls for real content generation; the writer-editor pipeline keeps the same draft-refine interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):


## Project Structure

```
two-agent-pipeline/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/twoagentpipeline/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TwoAgentPipelineExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EditorAgentWorker.java
│       ├── FinalOutputWorker.java
│       └── WriterAgentWorker.java
└── src/test/java/twoagentpipeline/workers/
    ├── EditorAgentWorkerTest.java        # 6 tests
    ├── FinalOutputWorkerTest.java        # 7 tests
    └── WriterAgentWorkerTest.java        # 5 tests
```
