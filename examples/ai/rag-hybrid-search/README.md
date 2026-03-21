# RAG Hybrid Search in Java Using Conductor: Vector + Keyword Search with Reciprocal Rank Fusion

Pure vector search returns documents about "network connectivity issues" when the user searched for the exact error code `ERR_CONNECTION_REFUSED`. Semantically similar, factually useless. Pure keyword search finds the error code but misses the doc titled "Troubleshooting refused connections" because it uses different words. Neither search strategy alone is good enough, and running both sequentially doubles your latency. This example builds a hybrid search pipeline using [Conductor](https://github.com/conductor-oss/conductor) that runs vector and BM25 keyword search in parallel, fuses the results with Reciprocal Rank Fusion, and generates an answer from the combined context.

## Neither Vector Nor Keyword Search Is Enough Alone

Vector search understands meaning but misses exact terms. searching for "ERR_CONNECTION_REFUSED" by semantic similarity might return documents about network errors in general, not the specific error code. Keyword search finds exact matches but misses synonyms, searching for "car insurance" won't find documents about "automobile coverage." Hybrid search runs both in parallel and combines the results.

Reciprocal rank fusion (RRF) merges the two ranked lists by giving each document a score based on its position in each list. Documents that rank highly in both searches float to the top.

## The Solution

**You write the vector search, keyword search, and RRF merge logic. Conductor handles the parallel execution, retries, and observability.**

Vector search and keyword search are independent workers. Conductor's `FORK_JOIN` runs both in parallel. An RRF merge worker combines the ranked results, and a generation worker produces the answer from the fused context. If the keyword index is slow, Conductor retries it without re-running the vector search.

### What You Write: Workers

Four workers implement hybrid search. Running vector similarity and BM25 keyword search in parallel via FORK_JOIN, merging results with Reciprocal Rank Fusion, and generating an answer from the fused context.

| Worker | Task | What It Does |
|---|---|---|
| **GenerateAnswerWorker** | `hs_generate_answer` | Answer generation worker. Generates an answer from the fused context documents. |
| **KeywordSearchWorker** | `hs_keyword_search` | Keyword (BM25) search worker. Simulates tokenizing the query and searching an inverted index. |
| **RrfMergeWorker** | `hs_rrf_merge` | Reciprocal Rank Fusion (RRF) merge worker. Deduplicates results from vector and keyword searches by document id, keep |
| **VectorSearchWorker** | `hs_vector_search` | Vector similarity search worker. Simulates embedding the query and searching an HNSW index (cosine similarity). |

Workers simulate LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode, the workflow and worker interfaces stay the same.

### The Workflow

```
FORK_JOIN
    ├── hs_vector_search
    └── hs_keyword_search
    │
    ▼
JOIN (wait for all branches)
hs_rrf_merge
    │
    ▼
hs_generate_answer

```

## Example Output

```
=== Example 146: RAG with Hybrid Search ===

Step 1: Registering task definitions...
  Registered: hs_vector_search, hs_keyword_search, hs_rrf_merge, hs_generate_answer

Step 2: Registering workflow 'rag_hybrid_search'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: d2a72be7-0193-7860-c36a-33aa47799338

  [vector] Embedding query: \"How does Conductor define and execute workflows?\"
  [vector] Searching HNSW index (cosine similarity)...
  [keyword] BM25 tokenizing: \"How does Conductor define and execute workflows?\"
  [keyword] Searching inverted index...
  [rrf] Applying reciprocal rank fusion (k=60)...
  [rrf] Merged 3 unique docs from 3 vector + 3 keyword results
  [llm] Answer (live OpenAI) from 2048 docs
  [llm] Generating answer from 2048 docs...

  Status: COMPLETED
  Output: {question=How does Conductor define and execute workflows?, answer=Conductor workflows are defined using a JSON DSL. Workers poll for tasks , sourceDocs=[item1, item2, item3], vectorCount=3, keywordCount=3}

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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/rag-hybrid-search-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(not set)_ | OpenAI API key. When set, GenerateAnswerWorker calls gpt-4o-mini instead of using simulated output |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/rag-hybrid-search-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rag_hybrid_search \
  --version 1 \
  --input '{"question": "How does Conductor define and execute workflows?"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rag_hybrid_search -s COMPLETED -c 5

```

## How to Extend

Each worker handles one search strategy. Swap in a real vector store for semantic search, Elasticsearch or Solr for BM25 keyword search, and the parallel search with reciprocal rank fusion runs unchanged.

- **GenerateAnswerWorker** (`hs_generate_answer`): send the merged context documents to an LLM (OpenAI GPT-4, Anthropic Claude) to generate a grounded answer
- **KeywordSearchWorker** (`hs_keyword_search`): tokenize the query and search a BM25 inverted index (Elasticsearch, Apache Solr, OpenSearch) for keyword-matching documents
- **RrfMergeWorker** (`hs_rrf_merge`): apply Reciprocal Rank Fusion to merge and deduplicate results from both vector and keyword searches into a single ranked list

Each search worker returns the same scored-result shape, so tuning RRF weights, adding new search strategies, or swapping the vector store requires no changes to the merge or generation workers.

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
rag-hybrid-search/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/raghybridsearch/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagHybridSearchExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GenerateAnswerWorker.java
│       ├── KeywordSearchWorker.java
│       ├── RrfMergeWorker.java
│       └── VectorSearchWorker.java
└── src/test/java/raghybridsearch/workers/
    ├── GenerateAnswerWorkerTest.java        # 4 tests
    ├── KeywordSearchWorkerTest.java        # 5 tests
    ├── RrfMergeWorkerTest.java        # 4 tests
    └── VectorSearchWorkerTest.java        # 4 tests

```
