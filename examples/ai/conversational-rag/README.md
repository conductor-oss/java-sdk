# Conversational RAG in Java Using Conductor :  Multi-Turn Chat with Context-Aware Retrieval

A Java Conductor workflow that powers multi-turn conversational retrieval-augmented generation. Each user message is embedded alongside recent conversation history, matched against a document store, and answered with grounded, context-aware responses. all while maintaining session state across turns. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate each stage as an independent worker,  you write the embedding, retrieval, and generation logic, Conductor handles sequencing, retries, durability, and observability.

## Why Conversational RAG Is Harder Than Single-Shot RAG

A single-turn RAG pipeline is straightforward: embed the query, retrieve documents, generate a response. But once users ask follow-up questions ("What about the pricing?" after asking "Tell me about product X"), the query alone is ambiguous. You need conversation history to resolve references and maintain coherence.

That means every request now depends on session state. loading prior turns, rewriting the query with conversational context, retrieving against the enriched query, generating a history-aware response, and persisting the new turn. Each of these steps can fail independently (embedding service timeout, vector store unreachable, LLM rate-limited), and if any step fails mid-conversation, you need to retry without corrupting session state or losing the user's place.

Without orchestration, you'd build this as a single method with nested try/catch blocks, manual session locking, and ad-hoc retry logic. code that's fragile, hard to test, and impossible to observe when a user reports "the bot forgot what I said."

## The Solution

**You write the session management, context-aware embedding, and generation logic. Conductor handles the multi-turn sequencing, retries, and observability.**

Each stage of the conversational RAG pipeline is an independent worker. loading history, embedding with context, retrieving documents, generating a response, saving the turn. Conductor sequences them, passes outputs between stages, retries on transient failures, and tracks every turn of every conversation for debugging. You get durable, observable multi-turn RAG without writing a line of orchestration code.

### What You Write: Workers

Five workers manage the full conversation turn. loading session history, embedding with conversational context, retrieving documents, generating a history-aware response, and persisting the new turn.

| Worker | Task | What It Does |
|---|---|---|
| **EmbedWithContextWorker** | `crag_embed_with_context` | Worker that embeds the user query with conversational context. Combines recent history with the current message to fo... |
| **GenerateWorker** | `crag_generate` | Worker that generates a response using user message, conversation history, and retrieved context documents. Returns a... |
| **LoadHistoryWorker** | `crag_load_history` | Worker that loads conversation history for a given session. Uses a static in-memory ConcurrentHashMap as the session ... |
| **RetrieveWorker** | `crag_retrieve` | Worker that retrieves relevant documents based on the contextual query. Returns 3 fixed documents with text and simil... |
| **SaveHistoryWorker** | `crag_save_history` | Worker that saves the current turn to conversation history. Appends user + assistant messages to the shared SESSION_S... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
crag_load_history
    │
    ▼
crag_embed_with_context
    │
    ▼
crag_retrieve
    │
    ▼
crag_generate
    │
    ▼
crag_save_history

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
java -jar target/conversational-rag-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(not set)_ | OpenAI API key. When set, embed and generate workers call OpenAI. When unset, all workers use demo output. |

### Live vs Demo Mode

- **Without `CONDUCTOR_OPENAI_API_KEY`**: All workers return deterministic demo output (default behavior, no API calls).
- **With `CONDUCTOR_OPENAI_API_KEY`**: EmbedWithContextWorker calls OpenAI Embeddings API (text-embedding-3-small) and GenerateWorker calls OpenAI Chat Completions (gpt-4o-mini). RetrieveWorker is demo-only because it requires a real vector database.
- If an OpenAI call fails, the worker automatically falls back to demo output.

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/conversational-rag-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow conversational_rag_workflow \
  --version 1 \
  --input '{"sessionId": "TEST-001", "userMessage": "Process this order for customer C-100"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w conversational_rag_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one conversation stage. swap the memory store for Redis or DynamoDB, plug in your LLM provider for generation, connect a real vector store for retrieval, and the conversational pipeline runs unchanged.

- **EmbedWithContextWorker** (`crag_embed_with_context`): swap the fixed embedding vector for a real call to OpenAI Embeddings, Cohere, or a local sentence-transformers model
- **GenerateWorker** (`crag_generate`): replace the demo response with a call to GPT-4, Claude, or any LLM, passing the retrieved context and conversation history as the prompt
- **LoadHistoryWorker** / **SaveHistoryWorker**. replace the in-memory ConcurrentHashMap with Redis, DynamoDB, or a database-backed session store for persistent conversation memory
- **RetrieveWorker** (`crag_retrieve`): swap the fixed documents for a real vector search against Pinecone, Weaviate, pgvector, or Elasticsearch

The session state contract is fixed across workers.  replace the in-memory store with Redis, swap the embedding provider, or upgrade the LLM, and the multi-turn pipeline runs unchanged.

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
conversational-rag/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/conversationalrag/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ConversationalRagExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EmbedWithContextWorker.java
│       ├── GenerateWorker.java
│       ├── LoadHistoryWorker.java
│       ├── RetrieveWorker.java
│       └── SaveHistoryWorker.java
└── src/test/java/conversationalrag/workers/
    ├── EmbedWithContextWorkerTest.java        # 7 tests
    ├── GenerateWorkerTest.java        # 6 tests
    ├── LoadHistoryWorkerTest.java        # 5 tests
    ├── RetrieveWorkerTest.java        # 5 tests
    └── SaveHistoryWorkerTest.java        # 6 tests

```
