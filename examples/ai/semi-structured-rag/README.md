# Semi-Structured RAG in Java Using Conductor :  Parallel Structured and Unstructured Search with Classification

A Java Conductor workflow that classifies a question's data needs (structured, unstructured, or both), searches structured sources (databases, APIs) and unstructured sources (document stores, vector databases) in parallel, merges the results, and generates a unified answer. Questions like "What's the average revenue for companies mentioned in our research reports?" need both SQL query results (structured) and document context (unstructured). Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate classification, parallel search, merging, and generation as independent workers .  you write the search logic, Conductor handles parallelism, retries, durability, and observability.

## When Answers Live in Both Tables and Documents

Enterprise knowledge spans structured data (databases, spreadsheets, APIs) and unstructured data (documents, emails, wikis). A question might need revenue figures from a database and context from an analyst report. Querying only one source gives an incomplete answer.

Semi-structured RAG classifies the question first (does it need structured data, unstructured data, or both?), then searches the appropriate sources in parallel. A merge step combines SQL results with document passages into a unified context for generation.

## The Solution

**You write the data classification and the structured/unstructured search logic. Conductor handles the parallel retrieval, merging, and observability.**

A classifier determines which sources to search. Conductor's `FORK_JOIN` runs structured and unstructured searches in parallel. A merge worker combines the results, and a generation worker produces the answer from the unified context. If the database query is slow, Conductor retries it without re-running the document search.

### What You Write: Workers

Five workers handle dual-source retrieval .  classifying the question's data needs, searching structured and unstructured sources in parallel via FORK_JOIN, merging both result types, and generating a unified answer.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyDataWorker** | `ss_classify_data` | Worker that classifies input data into structured fields and unstructured text chunks. Returns structuredFields (fiel... |
| **GenerateWorker** | `ss_generate` | Worker that generates a final answer from the question and merged context. Simulates LLM generation by producing a de... |
| **MergeResultsWorker** | `ss_merge_results` | Worker that merges structured and unstructured search results into a unified context string. Formats structured resul... |
| **SearchStructuredWorker** | `ss_search_structured` | Worker that searches structured data sources based on the classified structured fields. Returns results with field, v... |
| **SearchUnstructuredWorker** | `ss_search_unstructured` | Worker that searches unstructured text chunks for relevant passages. Returns results with chunkId, relevance score, a... |

Workers simulate LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode .  the workflow and worker interfaces stay the same.

### The Workflow

```
ss_classify_data
    │
    ▼
FORK_JOIN
    ├── ss_search_structured
    └── ss_search_unstructured
    │
    ▼
JOIN (wait for all branches)
ss_merge_results
    │
    ▼
ss_generate
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
java -jar target/semi-structured-rag-1.0.0.jar
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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key for embeddings and generation. When absent, workers use simulated responses. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/semi-structured-rag-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow semi_structured_rag_workflow \
  --version 1 \
  --input '{"question": "test-value", "dataContext": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w semi_structured_rag_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker targets one data type .  swap in real SQL queries for structured search, connect a vector store for unstructured search, implement cross-source merging, and the classify-search-merge-generate pipeline runs unchanged.

- **ClassifyDataWorker** (`ss_classify_data`): use an LLM or rule-based classifier to separate input data into structured fields (table/column references) and unstructured text chunks
- **SearchStructuredWorker** (`ss_search_structured`): query structured data sources (PostgreSQL, MySQL, or a data warehouse) using SQL generated from the classified structured fields
- **SearchUnstructuredWorker** (`ss_search_unstructured`): query a vector database (Pinecone, Weaviate, pgvector) to retrieve relevant unstructured text passages
- **MergeResultsWorker** (`ss_merge_results`): combine structured query results and unstructured text passages into a unified context with source type annotations
- **GenerateWorker** (`ss_generate`): send the merged structured + unstructured context to an LLM (GPT-4, Claude) to generate an answer that draws on both data types

Each search worker returns the same result shape regardless of data type, so swapping SQL databases, upgrading vector stores, or refining the classifier requires no changes to the merge or generation workers.

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
semi-structured-rag/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/semistructuredrag/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SemiStructuredRagExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClassifyDataWorker.java
│       ├── GenerateWorker.java
│       ├── MergeResultsWorker.java
│       ├── SearchStructuredWorker.java
│       └── SearchUnstructuredWorker.java
└── src/test/java/semistructuredrag/workers/
    ├── ClassifyDataWorkerTest.java        # 7 tests
    ├── GenerateWorkerTest.java        # 7 tests
    ├── MergeResultsWorkerTest.java        # 8 tests
    ├── SearchStructuredWorkerTest.java        # 6 tests
    └── SearchUnstructuredWorkerTest.java        # 7 tests
```
