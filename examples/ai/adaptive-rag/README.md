# Adaptive RAG in Java Using Conductor: Classify Query Complexity, Route to Optimal Retrieval Strategy

"What's the capital of France?" gets routed through the full RAG pipeline: embed, search, rerank, generate, burning tokens and adding 3 seconds of latency for an answer the LLM already knows. Meanwhile, "How did the 2008 financial crisis reshape European monetary policy over the following decade?" gets the same single-pass retrieval and produces a shallow, incomplete answer. One-size-fits-all RAG over-engineers simple questions and under-serves complex ones. This example builds an adaptive RAG pipeline using [Conductor](https://github.com/conductor-oss/conductor) that classifies each query by complexity and routes it to the optimal strategy, fast single-pass for factual lookups, multi-hop retrieval with chain-of-thought reasoning for analytical questions, and direct generation for creative queries.

## One RAG Strategy Does Not Fit All Questions

"What is the capital of France?" needs a single vector lookup and a short generation. "How did the 2008 financial crisis affect European monetary policy in the following decade?" needs multi-hop retrieval across multiple documents with a reasoning step to synthesize findings. Sending both through the same RAG pipeline either over-engineers simple questions (wasting tokens and latency) or under-serves complex ones (producing shallow, incomplete answers).

Adaptive RAG classifies each query to determine its complexity. Factual, multi-hop, or analytical, and routes to the retrieval strategy that fits. Simple queries get fast, single-pass retrieval. Multi-hop queries get iterative retrieval with intermediate reasoning. Analytical queries get specialized generation that synthesizes across sources. The classification happens once, and the routing is automatic.

## The Solution

**You write the query classifier and the per-complexity retrieval strategies. Conductor handles the routing, retries, and observability.**

`ClassifyWorker` examines the question and determines its complexity class. Simple, multi-hop, or analytical. A `SWITCH` task routes based on the classification: simple questions go to `SimpleRetrieveWorker` then `SimpleGenerateWorker` for direct retrieval and answer generation. Multi-hop questions go to `MultiHopRetrieveWorker` then `ReasonWorker` for iterative retrieval with intermediate reasoning steps. Analytical questions go to `AnalyticalGenerateWorker` for synthesis-heavy generation. Conductor makes this routing declarative and records which strategy was selected for each query.

### What You Write: Workers

Seven workers span three retrieval strategies: simple lookup, multi-hop reasoning, and creative generation, with a classifier that routes each query to the right path via a SWITCH task.

| Worker | Task | What It Does | Real / Notes |
|---|---|---|---|
| **ClassifyWorker** | `ar_classify` | Examines the question and determines its complexity class (`factual`, `analytical`, or `creative`) with a confidence score, routing it to the optimal retrieval strategy | Requires API key, or swap in a real LLM classifier (Claude, GPT-4) or a fine-tuned BERT model |
| **SimpleRetrieveWorker** | `ar_simple_ret` | Single-pass retrieval for factual queries. Returns basic document chunks from the vector store | Requires API key, or swap in Pinecone, Weaviate, Qdrant, or pgvector |
| **SimpleGenerateWorker** | `ar_simple_gen` | Produces a direct, concise answer from the retrieved documents (factual path) | Requires API key, or swap in Claude Messages API or OpenAI Chat Completions |
| **MultiHopRetrieveWorker** | `ar_mhop_ret` | Iterative multi-hop retrieval for analytical queries. Gathers documents across multiple hops to build a comprehensive evidence base | Requires API key, or swap in iterative vector store queries with query reformulation |
| **ReasoningWorker** | `ar_reason` | Builds a chain-of-thought reasoning trace from the multi-hop retrieved documents, connecting evidence across sources | Requires API key, or swap in Claude or GPT-4 with chain-of-thought prompting |
| **AnalyticalGenerateWorker** | `ar_anal_gen` | Synthesizes a comprehensive analytical answer from the reasoning chain and retrieved documents (analytical path) | Requires API key, or swap in Claude or GPT-4 with synthesis prompting |
| **CreativeGenerateWorker** | `ar_creative_gen` | Produces a free-form creative answer without retrieval (default/creative path) | Requires API key, or swap in any LLM with creative generation settings |

Workers require CONDUCTOR_OPENAI_API_KEY. Retrieval workers use Jaccard similarity over bundled documents.

### The Workflow

```
ar_classify
    │
    ▼
SWITCH (sw_ref)
    ├── factual: ar_simple_ret -> ar_simple_gen
    ├── analytical: ar_mhop_ret -> ar_reason -> ar_anal_gen
    └── default: ar_creative_gen

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
java -jar target/adaptive-rag-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(required)_ | OpenAI API key. When set, classify, generate, and reasoning workers call OpenAI (gpt-4o-mini). Workers throw IllegalStateException if not set. |

### API Key Requirement

All LLM workers (classify, generate, reasoning) require CONDUCTOR_OPENAI_API_KEY.
Retrieval workers (SimpleRetrieveWorker, MultiHopRetrieveWorker) use Jaccard similarity over bundled documents.; no vector database needed.


## Example Output

```
=== Adaptive RAG Demo: Route by Query Complexity ===

Step 1: Registering task definitions...
  Registered: ar_classify, ar_simple_ret, ar_simple_gen, ar_mhop_ret, ar_reason,
    ar_anal_gen, ar_creative_gen

Step 2: Registering workflow 'adaptive_rag'...
  Workflow registered.

Step 3: Starting workers...
  7 workers polling.

Step 4: Starting workflow...
  [classify] Question: "What is Conductor?" -> factual (confidence: 0.92)
  [simple_ret] Retrieved 2 documents for factual query
  [simple_gen] Generated direct answer from 2 documents

  Workflow ID: a5b6c7d8-...

Step 5: Waiting for completion...
  Status: COMPLETED
  Query type: factual
  Confidence: 0.92

--- Adaptive RAG Pattern ---
  Query classified as 'factual' with 0.92 confidence.
  Routed to simple retrieval + direct generation (fast path).

Result: PASSED

```

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/adaptive-rag-1.0.0.jar --workers

```

Then in a separate terminal:

Factual query (simple retrieval + direct generation):

```bash
conductor workflow start \
  --workflow adaptive_rag \
  --version 1 \
  --input '{"question": "What is Conductor?"}'

```

Analytical query (multi-hop retrieval + reasoning + synthesis):

```bash
conductor workflow start \
  --workflow adaptive_rag \
  --version 1 \
  --input '{"question": "How does Conductor compare to Temporal for long-running workflow orchestration?"}'

```

Creative query (free-form generation, no retrieval):

```bash
conductor workflow start \
  --workflow adaptive_rag \
  --version 1 \
  --input '{"question": "Write a poem about microservices orchestration"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w adaptive_rag -s COMPLETED -c 5

```

## The Adaptive RAG Pipeline

Not all questions need the same retrieval strategy. Adaptive RAG classifies each query and routes to the optimal pipeline:

1. **Classify** (`ar_classify`): An LLM-based classifier examines the question and determines its complexity: `factual` (single-hop lookup), `analytical` (multi-source synthesis), or `creative` (free-form generation). The classification includes a confidence score.

2. **Route** (SWITCH): Conductor's SWITCH task routes based on the classification:
   - **Factual**: `ar_simple_ret` -> `ar_simple_gen`. Single-pass vector retrieval, then direct answer generation. Fast and cheap.
   - **Analytical**: `ar_mhop_ret` -> `ar_reason` -> `ar_anal_gen`. Multi-hop retrieval across multiple documents, intermediate chain-of-thought reasoning, then synthesis. Thorough but more expensive.
   - **Creative** (default): `ar_creative_gen`. Free-form generation without retrieval. No vector store cost.

This saves tokens and latency on simple questions while giving complex questions the depth they need. Every execution records which strategy was selected, so you can analyze classification accuracy and strategy effectiveness over time.

## How to Extend

Each worker handles one retrieval strategy or classification step. Plug in a real LLM classifier, connect vector stores for simple and multi-hop retrieval, and add chain-of-thought reasoning via Claude or GPT-4, and the adaptive routing runs unchanged.

- **ClassifyWorker** (`ar_classify`): use a real LLM classifier (Claude, GPT-4) to determine query complexity, or fine-tune a small BERT model on labeled query complexity data for faster, cheaper classification
- **SimpleRetrieveWorker** (`ar_simple_ret`): query a real vector database (Pinecone, Weaviate, Qdrant, pgvector) with the embedded question to retrieve relevant document chunks
- **MultiHopRetrieveWorker** (`ar_mhop_ret`): implement iterative retrieval with query reformulation: retrieve initial documents, extract key entities, reformulate the query, and retrieve additional documents across multiple hops
- **ReasoningWorker** (`ar_reason`): call Claude or GPT-4 with chain-of-thought prompting to build a structured reasoning trace from the multi-hop evidence
- **AnalyticalGenerateWorker** (`ar_anal_gen`): call a real LLM with the reasoning chain and retrieved context, using a specialized analytical prompt that instructs cross-source synthesis and evidence-based reasoning

Each worker's interface is fixed. Replace the classifier model, upgrade the retrieval strategy, or swap LLM providers, and the adaptive routing runs without modification.

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
adaptive-rag/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/adaptiverag/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AdaptiveRagExample.java      # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyticalGenerateWorker.java  # Synthesis from reasoning chain
│       ├── ClassifyWorker.java            # Query complexity classifier
│       ├── CreativeGenerateWorker.java    # Free-form generation (no retrieval)
│       ├── MultiHopRetrieveWorker.java    # Iterative multi-hop retrieval
│       ├── ReasoningWorker.java           # Chain-of-thought reasoning
│       ├── SimpleGenerateWorker.java      # Direct answer from documents
│       └── SimpleRetrieveWorker.java      # Single-pass vector retrieval
└── src/test/java/adaptiverag/workers/
    ├── AnalyticalGenerateWorkerTest.java
    ├── ClassifyWorkerTest.java
    ├── CreativeGenerateWorkerTest.java
    ├── MultiHopRetrieveWorkerTest.java
    ├── ReasoningWorkerTest.java
    ├── SimpleGenerateWorkerTest.java
    └── SimpleRetrieveWorkerTest.java

```
