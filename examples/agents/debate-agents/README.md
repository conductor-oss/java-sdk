# Debate Agents in Java Using Conductor :  Pro and Con Arguments in Iterative Rounds with Moderation

Debate Agents. PRO and CON agents argue over a topic for multiple rounds, then a moderator summarizes. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .## Exploring Both Sides of an Issue Systematically

Asking an LLM for pros and cons in a single call produces a superficial list. A structured debate produces deeper analysis: the pro agent makes a strong argument, the con agent directly counters it, and the pro agent responds to the counter .  each round sharpening the analysis. After three rounds, the moderator has six substantive arguments to synthesize into a balanced summary.

The debate loop requires careful state management: each agent needs access to all prior arguments (not just the last one), the round counter must track progress, and the moderator must have the complete debate transcript. Without orchestration, managing the accumulating debate history across loop iterations, handling a timeout mid-debate without losing prior rounds, and generating the final synthesis from all arguments is complex state management code.

## The Solution

**You write the argumentation and moderation logic. Conductor handles the debate loop, transcript accumulation, and round management.**

`SetTopicWorker` establishes the debate topic and the positions each agent will argue. A `DO_WHILE` loop iterates through debate rounds: `AgentProWorker` presents arguments supporting the position (with rebuttals to prior con arguments), and `AgentConWorker` presents counterarguments (addressing the pro agent's latest points). After the configured number of rounds, `ModeratorSummarizeWorker` analyzes the complete debate transcript, identifies the strongest arguments from each side, and produces a balanced summary with a verdict. Conductor manages the accumulating transcript across rounds and records each argument for analysis.

### What You Write: Workers

Four workers stage the debate. Setting the topic, then iterating PRO and CON arguments through multiple rounds before the moderator synthesizes a verdict.

| Worker | Task | What It Does |
|---|---|---|
| **AgentConWorker** | `da_agent_con` | CON agent .  argues against the debate topic each round. Uses deterministic argument selection based on the round input. |
| **AgentProWorker** | `da_agent_pro` | PRO agent .  argues in favor of the debate topic each round. Uses deterministic argument selection based on the round .. |
| **ModeratorSummarizeWorker** | `da_moderator_summarize` | Moderator agent .  summarizes the debate and delivers a verdict after all rounds. |
| **SetTopicWorker** | `da_set_topic` | Sets up the debate topic and defines the two sides (PRO and CON). |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode .  the agent workflow stays the same.

### The Workflow

```
da_set_topic
    │
    ▼
DO_WHILE
    └── da_agent_pro
    └── da_agent_con
    │
    ▼
da_moderator_summarize
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
java -jar target/debate-agents-1.0.0.jar
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
java -jar target/debate-agents-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow debate_agents_demo \
  --version 1 \
  --input '{"topic": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w debate_agents_demo -s COMPLETED -c 5
```

## How to Extend

Each agent takes one side of the debate. Replace scripted arguments with LLM-powered reasoning (GPT-4 or Claude with position-specific prompts), add an impartial LLM moderator, and the multi-round debate workflow runs unchanged.

- **AgentProWorker/AgentConWorker** (`da_agent_pro/con`): use GPT-4 or Claude with system prompts that instruct the agent to argue a specific position, reference prior arguments by ID, and use evidence-based reasoning
- **ModeratorSummarizeWorker** (`da_moderator_summarize`): use a different LLM than the debaters to provide impartial moderation, with structured scoring rubrics for argument quality, evidence use, and rebuttal effectiveness
- **SetTopicWorker** (`da_set_topic`): support multiple debate formats: Oxford-style (structured motions), Socratic (question-driven), or Devil's advocate (one side intentionally contrarian)

Replace with real LLM argumentation; the debate loop preserves the same round-based argument-and-rebuttal interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
debate-agents/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/debateagents/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DebateAgentsExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AgentConWorker.java
│       ├── AgentProWorker.java
│       ├── ModeratorSummarizeWorker.java
│       └── SetTopicWorker.java
└── src/test/java/debateagents/workers/
    ├── AgentConWorkerTest.java        # 12 tests
    ├── AgentProWorkerTest.java        # 12 tests
    ├── ModeratorSummarizeWorkerTest.java        # 10 tests
    └── SetTopicWorkerTest.java        # 6 tests
```
