# RAG Multi-Query in Java Using Conductor :  Expand, Search in Parallel, Deduplicate, Generate

A Java Conductor workflow that expands a single user question into multiple query variants (paraphrases, sub-questions, alternative phrasings), searches the vector store with each variant in parallel, deduplicates the combined results, and generates an answer from the enriched context. Multiple queries cast a wider retrieval net than a single query. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate query expansion, parallel search, deduplication, and generation as independent workers.  you write the expansion and search logic, Conductor handles parallelism, retries, durability, and observability.

## One Question, Multiple Search Angles

A single vector search query captures one representation of the user's intent. By expanding the question into multiple variants .  "How do I authenticate?" becomes "authentication setup guide", "login configuration steps", "auth token implementation" ,  you retrieve documents that match different aspects of the same intent. The parallel searches return overlapping and complementary results that are deduplicated before generation.

## The Solution

**You write the query expansion, search, and deduplication logic. Conductor handles the parallel execution, retries, and observability.**

A query expander generates three variants. Conductor's `FORK_JOIN` searches with all three in parallel. A deduplication worker removes duplicate documents across result sets, and a generation worker produces the answer from the combined, deduplicated context.

### What You Write: Workers

Six workers implement multi-query retrieval .  expanding the original question into three variant queries, searching for each in parallel via FORK_JOIN, deduplicating the combined results, and generating an answer from the broadened context.

| Worker | Task | What It Does |
|---|---|---|
| **DedupResultsWorker** | `mq_dedup_results` | Worker that deduplicates search results from multiple query branches. Takes results1, results2, results3 and returns ... |
| **ExpandQueriesWorker** | `mq_expand_queries` | Worker that expands a user question into multiple search query variants. |
| **GenerateAnswerWorker** | `mq_generate_answer` | Worker that generates a final answer from deduplicated context documents. |
| **SearchQ1Worker** | `mq_search_q1` | Worker that searches the knowledge base with query variant 1. Returns documents d1 and d4 (d1 overlaps with q2, d4 ov... |
| **SearchQ2Worker** | `mq_search_q2` | Worker that searches the knowledge base with query variant 2. Returns documents d1, d7, d9 (d1 overlaps with q1). |
| **SearchQ3Worker** | `mq_search_q3` | Worker that searches the knowledge base with query variant 3. Returns documents d4 and d11 (d4 overlaps with q1). |

Workers simulate LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode .  the workflow and worker interfaces stay the same.

### The Workflow

```
mq_expand_queries
    │
    ▼
FORK_JOIN
    ├── mq_search_q1
    ├── mq_search_q2
    └── mq_search_q3
    │
    ▼
JOIN (wait for all branches)
mq_dedup_results
    │
    ▼
mq_generate_answer

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
java -jar target/rag-multi-query-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(not set)_ | OpenAI API key. When set, ExpandQueriesWorker and GenerateAnswerWorker call gpt-4o-mini instead of using simulated output |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/rag-multi-query-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rag_multi_query \
  --version 1 \
  --input '{"question": "What is workflow orchestration?"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rag_multi_query -s COMPLETED -c 5

```

## How to Extend

Each worker handles one multi-query step .  swap in an LLM for query expansion, connect a real vector store for parallel variant searches, implement deduplication by document ID, and the expand-search-deduplicate-generate pipeline runs unchanged.

- **ExpandQueriesWorker** (`mq_expand_queries`): use an LLM (GPT-4, Claude) to rephrase the user's question into multiple search query variants for broader retrieval coverage
- **SearchQ1Worker** (`mq_search_q1`): query a vector database (Pinecone, Weaviate, pgvector) with the first query variant
- **SearchQ2Worker** (`mq_search_q2`): query the same or a different index with the second query variant for diverse result coverage
- **SearchQ3Worker** (`mq_search_q3`): query the same or a different index with the third query variant for maximum recall
- **DedupResultsWorker** (`mq_dedup_results`): deduplicate results from all query branches by document ID, keeping the highest-scoring occurrence of each document
- **GenerateAnswerWorker** (`mq_generate_answer`): send the deduplicated context documents to an LLM (OpenAI GPT-4, Anthropic Claude) to generate a comprehensive answer

Each search worker returns the same document/score shape, so adjusting the number of query variants or changing the deduplication strategy requires no changes to existing workers.

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
rag-multi-query/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragmultiquery/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagMultiQueryExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DedupResultsWorker.java
│       ├── ExpandQueriesWorker.java
│       ├── GenerateAnswerWorker.java
│       ├── SearchQ1Worker.java
│       ├── SearchQ2Worker.java
│       └── SearchQ3Worker.java
└── src/test/java/ragmultiquery/workers/
    ├── DedupResultsWorkerTest.java        # 5 tests
    ├── ExpandQueriesWorkerTest.java        # 5 tests
    ├── GenerateAnswerWorkerTest.java        # 5 tests
    ├── SearchQ1WorkerTest.java        # 4 tests
    ├── SearchQ2WorkerTest.java        # 4 tests
    └── SearchQ3WorkerTest.java        # 4 tests

```
