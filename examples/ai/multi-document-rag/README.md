# Multi-Document RAG in Java Using Conductor :  Parallel Search Across API Docs, Tutorials, and Forums

A Java Conductor workflow that searches three document collections simultaneously. API documentation, tutorials, and community forums. merges the results by relevance, and generates an answer grounded in all three sources. Conductor's `FORK_JOIN` runs the three searches in parallel, cutting latency to the speed of the slowest collection instead of the sum of all three. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate parallel search, result merging, and generation as independent workers,  you write the search and generation logic, Conductor handles parallelism, retries, durability, and observability.

## Answers That Span Multiple Knowledge Sources

A developer question like "How do I paginate API results?" might have the answer spread across three places: the API reference (parameter names and types), a tutorial (step-by-step walkthrough), and a forum thread (common gotchas and workarounds). Searching a single collection misses context. Searching all three sequentially triples the latency.

The solution is parallel search: embed the question once, then search API docs, tutorials, and forums simultaneously. After all three searches complete, merge the results by relevance score. giving priority to API docs for technical accuracy while including tutorial context and forum insights. The merged context feeds into the LLM for a comprehensive answer that cites multiple source types.

Without orchestration, parallel search means managing thread pools, handling partial failures (forums are down but API docs responded), and waiting for all results before merging. code that's hard to get right and impossible to observe.

## The Solution

**You write the per-collection search and merge logic. Conductor handles the parallel execution, retries, and observability.**

Each search is an independent worker. one per collection. Conductor's `FORK_JOIN` runs all three in parallel and waits for all to complete. A merge worker combines the results, and a generation worker produces the answer. If the forum search times out, Conductor retries it independently without re-running the API docs or tutorial searches.

### What You Write: Workers

Six workers implement cross-collection RAG. embedding the query, then searching API docs, tutorials, and forums in parallel via FORK_JOIN, merging the ranked results, and generating an answer from the unified context.

| Worker | Task | What It Does |
|---|---|---|
| **EmbedWorker** | `mdrag_embed` | Worker that generates a fixed embedding vector for a query. |
| **GenerateWorker** | `mdrag_generate` | Worker that generates an answer from the merged context. |
| **MergeResultsWorker** | `mdrag_merge_results` | Worker that merges results from api_docs, tutorials, and forums collections, sorts by score descending, and returns m... |
| **SearchApiDocsWorker** | `mdrag_search_api_docs` | Worker that searches the API docs collection and returns 2 results. |
| **SearchForumsWorker** | `mdrag_search_forums` | Worker that searches the forums collection and returns 1 result. |
| **SearchTutorialsWorker** | `mdrag_search_tutorials` | Worker that searches the tutorials collection and returns 2 results. |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
mdrag_embed
    │
    ▼
FORK_JOIN
    ├── mdrag_search_api_docs
    ├── mdrag_search_tutorials
    └── mdrag_search_forums
    │
    ▼
JOIN (wait for all branches)
mdrag_merge_results
    │
    ▼
mdrag_generate

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
java -jar target/multi-document-rag-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(not set)_ | OpenAI API key. When set, embed and generate workers call OpenAI. When unset, all workers use simulated output. |

### Live vs Simulated Mode

- **Without `CONDUCTOR_OPENAI_API_KEY`**: All workers return deterministic simulated output (default behavior, no API calls).
- **With `CONDUCTOR_OPENAI_API_KEY`**: EmbedWorker calls OpenAI Embeddings API (text-embedding-3-small) and GenerateWorker calls OpenAI Chat Completions (gpt-4o-mini). Search workers (SearchApiDocsWorker, SearchTutorialsWorker, SearchForumsWorker) remain simulated because they require a real vector database.
- If an OpenAI call fails, the worker automatically falls back to simulated output.

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/multi-document-rag-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow multi_document_rag_workflow \
  --version 1 \
  --input '{"question": "What is workflow orchestration?"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w multi_document_rag_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker searches one knowledge source. connect each to its own vector store collection or search index, implement reciprocal rank fusion for merging, and the parallel search-merge-generate workflow runs unchanged.

- **EmbedWorker** (`mdrag_embed`): swap in a real embedding model (OpenAI, Cohere, sentence-transformers)
- **SearchApiDocsWorker** / **SearchTutorialsWorker** / **SearchForumsWorker**. connect each to its own vector store collection or search index
- **MergeResultsWorker** (`mdrag_merge_results`): implement reciprocal rank fusion or weighted scoring to combine results across collections
- **GenerateWorker** (`mdrag_generate`): swap in a real LLM call with source attribution in the prompt

Each search worker returns the same result shape, so adding new document collections or swapping search backends requires only a new worker and a fork branch. no changes to existing workers.

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
multi-document-rag/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/multidocumentrag/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MultiDocumentRagExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EmbedWorker.java
│       ├── GenerateWorker.java
│       ├── MergeResultsWorker.java
│       ├── SearchApiDocsWorker.java
│       ├── SearchForumsWorker.java
│       └── SearchTutorialsWorker.java
└── src/test/java/multidocumentrag/workers/
    ├── EmbedWorkerTest.java        # 3 tests
    ├── GenerateWorkerTest.java        # 3 tests
    ├── MergeResultsWorkerTest.java        # 3 tests
    ├── SearchApiDocsWorkerTest.java        # 2 tests
    ├── SearchForumsWorkerTest.java        # 2 tests
    └── SearchTutorialsWorkerTest.java        # 2 tests

```
