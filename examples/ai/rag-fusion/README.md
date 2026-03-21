# RAG Fusion in Java Using Conductor :  Multi-Query Parallel Search with Reciprocal Rank Fusion

A Java Conductor workflow that implements RAG Fusion. rewriting the user's question into multiple search queries (different phrasings, perspectives, and specificity levels), searching the vector store with each query in parallel, and fusing the results using reciprocal rank fusion (RRF) to produce a better-ranked document set than any single query would yield. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate query rewriting, parallel search, result fusion, and generation as independent workers,  you write the rewriting and fusion logic, Conductor handles parallelism, retries, durability, and observability.

## Why One Query Is Not Enough

A single search query captures one perspective on the user's question. Rephrasing the same question in different ways. more specific, more general, using synonyms, from different angles,  retrieves different relevant documents. RAG Fusion rewrites the original question into multiple queries, searches with each one in parallel, and merges the results using reciprocal rank fusion, which gives higher scores to documents that appear in multiple result sets.

The parallel search step is critical: three sequential searches take 3x the latency, but three parallel searches take 1x. After fusion, the merged results feed into generation for a more comprehensive answer.

## The Solution

**You write the query rewriting, parallel search, and rank fusion logic. Conductor handles the parallel execution, retries, and observability.**

A query rewriter generates multiple query variants. Conductor's `FORK_JOIN` searches with all three in parallel. A fusion worker applies reciprocal rank fusion to merge and re-rank the results. A generation worker produces the answer from the fused context. If any individual search times out, Conductor retries it independently.

### What You Write: Workers

Six workers implement RAG fusion. rewriting the original query into multiple variants, running three parallel searches via FORK_JOIN, fusing the results with Reciprocal Rank Fusion, and generating an answer from the combined context.

| Worker | Task | What It Does |
|---|---|---|
| **FuseResultsWorker** | `rf_fuse_results` | Worker that fuses results from multiple search engines using Reciprocal Rank Fusion (RRF). Uses k=60, score = sum(1/(... |
| **GenerateAnswerWorker** | `rf_generate_answer` | Worker that generates an answer using the original question and fused context from multiple search engines. Combines ... |
| **RewriteQueriesWorker** | `rf_rewrite_queries` | Worker that rewrites the original question into 3 variant queries for multi-perspective retrieval. Returns determinis... |
| **SearchV1Worker** | `rf_search_v1` | Search engine V1 worker. Takes a query and variantIndex, returns ranked results with id, text, and rank. Simulates a ... |
| **SearchV2Worker** | `rf_search_v2` | Search engine V2 worker. Takes a query and variantIndex, returns ranked results with id, text, and rank. Simulates a ... |
| **SearchV3Worker** | `rf_search_v3` | Search engine V3 worker. Takes a query and variantIndex, returns ranked results with id, text, and rank. Simulates a ... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
rf_rewrite_queries
    │
    ▼
FORK_JOIN
    ├── rf_search_v1
    ├── rf_search_v2
    └── rf_search_v3
    │
    ▼
JOIN (wait for all branches)
rf_fuse_results
    │
    ▼
rf_generate_answer

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
java -jar target/rag-fusion-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(not set)_ | OpenAI API key. When set, GenerateAnswerWorker and RewriteQueriesWorker call gpt-4o-mini instead of using demo output |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/rag-fusion-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rag_fusion_workflow \
  --version 1 \
  --input '{"question": "What is workflow orchestration?"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rag_fusion_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one fusion stage. swap in an LLM for query rewriting, connect a real vector store for parallel multi-query search, implement reciprocal rank fusion for merging, and the rewrite-search-fuse-generate pipeline runs unchanged.

- **RewriteQueriesWorker** (`rf_rewrite_queries`): use an LLM (GPT-4, Claude) to rephrase the original question into multiple query variants for multi-perspective retrieval
- **SearchV1Worker** (`rf_search_v1`): query a keyword-based search engine (Elasticsearch BM25, Apache Solr) with the rewritten query variant
- **SearchV2Worker** (`rf_search_v2`): query a vector search engine (Pinecone, Weaviate, Qdrant) with an embedded query variant for semantic retrieval
- **SearchV3Worker** (`rf_search_v3`): query a hybrid search engine (Elasticsearch with dense+sparse vectors, Vespa) combining keyword and semantic signals
- **FuseResultsWorker** (`rf_fuse_results`): apply Reciprocal Rank Fusion (RRF) with configurable k parameter across all search engine results, deduplicating by document ID
- **GenerateAnswerWorker** (`rf_generate_answer`): send the fused, top-ranked documents as context to an LLM (GPT-4, Claude) to generate a comprehensive answer

Each search worker returns the same scored-document shape, so adding more search variants or changing the fusion algorithm requires no modifications to existing workers.

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
rag-fusion/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragfusion/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagFusionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FuseResultsWorker.java
│       ├── GenerateAnswerWorker.java
│       ├── RewriteQueriesWorker.java
│       ├── SearchV1Worker.java
│       ├── SearchV2Worker.java
│       └── SearchV3Worker.java
└── src/test/java/ragfusion/workers/
    ├── FuseResultsWorkerTest.java        # 7 tests
    ├── GenerateAnswerWorkerTest.java        # 7 tests
    ├── RewriteQueriesWorkerTest.java        # 6 tests
    ├── SearchV1WorkerTest.java        # 7 tests
    ├── SearchV2WorkerTest.java        # 7 tests
    └── SearchV3WorkerTest.java        # 7 tests

```
