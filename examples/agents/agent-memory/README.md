# Agent Memory in Java Using Conductor :  Load, Think, Update, and Respond with Persistent Context

Agent with Memory .  loads conversation history, thinks with context, updates memory, and responds. Uses [Conductor](https://github.

## Stateless Agents Forget Everything Between Messages

Without memory, every agent interaction starts from zero. A user says "I'm interested in transformers" and gets a great explanation. Next message: "How do they compare to RNNs?", and the agent has no idea what "they" refers to because the previous context is gone. The user has to repeat themselves every time.

Agent memory solves this by maintaining a persistent store of facts, topics, and context across conversations. Before responding, the agent loads relevant memories ("user is learning about neural architectures, discussed transformers last time"), reasons with that context, updates the store with any new facts from the current exchange, and generates a response that builds on prior conversations. Each of these steps can fail independently .  the memory store might be temporarily unavailable, the reasoning step might time out, and you need to handle failures without corrupting the memory state.

## The Solution

**You write the memory retrieval, reasoning, and persistence logic. Conductor handles sequencing, state management, and failure recovery.**

`LoadMemoryWorker` retrieves the user's memory store .  prior topics, facts, and conversation context ,  from a session-keyed data store. `AgentThinkWorker` reasons about the current message using both the user input and loaded memories, identifying relevant prior context and planning a response strategy. `UpdateMemoryWorker` extracts new facts from the current exchange and persists them to the memory store for future conversations. `AgentRespondWorker` generates the final response informed by all prior reasoning and memories. Conductor ensures memories are loaded before reasoning, updated before responding, and that a failed memory update doesn't block the response.

### What You Write: Workers

Four workers manage stateful conversations. Loading memory, reasoning with context, updating the memory store, and generating a context-aware response.

| Worker | Task | What It Does |
|---|---|---|
| **AgentRespondWorker** | `am_agent_respond` | Generates the final response based on thoughts, relevant facts, and updated memory. Returns the response string, conf... |
| **AgentThinkWorker** | `am_agent_think` | Analyzes user message in the context of conversation history and user profile. Returns thoughts, relevantFacts, and f... |
| **LoadMemoryWorker** | `am_load_memory` | Loads conversation history and user profile from memory for the given user. Returns conversationHistory (list of entr... |
| **UpdateMemoryWorker** | `am_update_memory` | Updates the memory store with the latest interaction data. Returns a memorySnapshot and memorySize. |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode .  the agent workflow stays the same.

### The Workflow

```
am_load_memory
    │
    ▼
am_agent_think
    │
    ▼
am_update_memory
    │
    ▼
am_agent_respond

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
java -jar target/agent-memory-1.0.0.jar

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
java -jar target/agent-memory-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow agent_memory_demo \
  --version 1 \
  --input '{"userId": "TEST-001", "userMessage": "Process this order for customer C-100"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w agent_memory_demo -s COMPLETED -c 5

```

## How to Extend

Each worker handles one phase of the memory cycle. Swap in Redis or a vector store (Pinecone, Weaviate) for memory persistence and an LLM for context-aware reasoning, and the load-think-update-respond workflow runs unchanged.

- **LoadMemoryWorker** (`am_load_memory`): replace the in-memory ConcurrentHashMap with Redis for cross-instance memory sharing, or use a vector store (Pinecone, Weaviate) to retrieve semantically relevant memories instead of loading all history
- **AgentThinkWorker** (`am_agent_think`): call GPT-4 or Claude with the loaded memories as context, using system prompts that instruct the model to reference prior conversations and build on established topics
- **UpdateMemoryWorker** (`am_update_memory`): use an LLM to extract structured facts from the conversation (entities, preferences, topics discussed) and store them in a knowledge graph or typed memory schema

Wire in Redis or a vector store for real memory persistence; the conversation workflow keeps the same load-think-update-respond interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
agent-memory/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/agentmemory/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AgentMemoryExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AgentRespondWorker.java
│       ├── AgentThinkWorker.java
│       ├── LoadMemoryWorker.java
│       └── UpdateMemoryWorker.java
└── src/test/java/agentmemory/workers/
    ├── AgentRespondWorkerTest.java        # 8 tests
    ├── AgentThinkWorkerTest.java        # 8 tests
    ├── LoadMemoryWorkerTest.java        # 9 tests
    └── UpdateMemoryWorkerTest.java        # 8 tests

```
