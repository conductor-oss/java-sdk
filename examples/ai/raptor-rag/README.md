# RAPTOR RAG in Java Using Conductor :  Hierarchical Document Summarization Tree for Multi-Level Retrieval

A Java Conductor workflow that implements RAPTOR (Recursive Abstractive Processing for Tree-Organized Retrieval). chunking a document, generating leaf-level summaries, clustering and summarizing those into higher-level abstractions, then searching across all levels of the summary tree to find context at the right granularity for the question. This produces both fine-grained (chunk-level) and abstract (theme-level) context for richer answers. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate chunking, leaf summarization, hierarchical clustering, tree search, and generation as independent workers.

## Beyond Flat Chunk Retrieval

Standard RAG retrieves individual chunks. fine-grained text fragments that match the query. But some questions need high-level understanding: "What are the main themes of this paper?" can't be answered by any single chunk. RAPTOR builds a tree of summaries: leaf nodes are chunk-level summaries, and higher nodes are summaries of summaries (clusters of related chunks). At query time, the tree is searched at all levels, retrieving both specific details and broad themes.

The pipeline chunks the document, summarizes each chunk (leaf level), clusters similar summaries and creates higher-level abstractions, then searches across all tree levels for the most relevant context at the right granularity.

## The Solution

**You write the chunking, summarization, clustering, and tree search logic. Conductor handles the hierarchical pipeline, retries, and observability.**

Each stage of the RAPTOR pipeline is an independent worker. document chunking, leaf summary generation, hierarchical clustering/summarization, tree search, and answer generation. Conductor sequences them, retries the LLM summarization if rate-limited, and tracks the full tree construction and search for debugging.

### What You Write: Workers

Five workers build the RAPTOR tree pipeline. chunking documents, generating leaf-level summaries, clustering into hierarchical abstractions, searching across all tree levels, and generating an answer from multi-granularity context.

| Worker | Task | What It Does |
|---|---|---|
| **ChunkDocsWorker** | `rp_chunk_docs` | Worker that chunks a document into smaller segments for RAPTOR tree construction. Takes documentText and returns 6 fi... |
| **ClusterSummariesWorker** | `rp_cluster_summaries` | Worker that builds cluster-level summaries from leaf summaries. Produces 2 cluster summaries at level 1 and a single ... |
| **GenerateWorker** | `rp_generate` | Worker that generates an answer using the question and multi-level context retrieved from the RAPTOR tree. Combines c... |
| **LeafSummariesWorker** | `rp_leaf_summaries` | Worker that creates leaf-level summaries from document chunks. Groups related chunks and produces 4 leaf summaries at... |
| **TreeSearchWorker** | `rp_tree_search` | Worker that searches the RAPTOR tree for context relevant to a question. Traverses the tree top-down (root -> cluster... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
rp_chunk_docs
    │
    ▼
rp_leaf_summaries
    │
    ▼
rp_cluster_summaries
    │
    ▼
rp_tree_search
    │
    ▼
rp_generate

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
java -jar target/raptor-rag-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(not set)_ | OpenAI API key. When set, GenerateWorker calls gpt-4o-mini instead of using demo output |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/raptor-rag-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow raptor_rag_workflow \
  --version 1 \
  --input '{"documentText": "Process this order for customer C-100", "question": "What is workflow orchestration?"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w raptor_rag_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one tree-building step. swap in a real LLM for recursive summarization, connect a vector store for multi-level tree search, and the chunk-summarize-cluster-search-generate RAPTOR pipeline runs unchanged.

- **ChunkDocsWorker** (`rp_chunk_docs`): use a text splitter (LangChain RecursiveCharacterTextSplitter, sentence-transformers) to chunk documents into segments sized for RAPTOR tree construction
- **LeafSummariesWorker** (`rp_leaf_summaries`): use an LLM (GPT-4, Claude) to generate leaf-level summaries from document chunk groups, forming the base layer of the RAPTOR tree
- **ClusterSummariesWorker** (`rp_cluster_summaries`): use an LLM to build hierarchical cluster summaries from leaf summaries, creating the intermediate and root levels of the RAPTOR tree
- **TreeSearchWorker** (`rp_tree_search`): traverse the RAPTOR tree top-down (root to clusters to leaves) using embedding similarity at each level for multi-granularity retrieval
- **GenerateWorker** (`rp_generate`): send multi-level RAPTOR tree context (root summaries + cluster details + leaf excerpts) to an LLM for comprehensive answer generation

Each tree-building worker preserves its summary/cluster contract, so swapping the summarization model or the clustering algorithm requires no changes to the search or generation steps.

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
raptor-rag/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/raptorrag/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RaptorRagExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ChunkDocsWorker.java
│       ├── ClusterSummariesWorker.java
│       ├── GenerateWorker.java
│       ├── LeafSummariesWorker.java
│       └── TreeSearchWorker.java
└── src/test/java/raptorrag/workers/
    ├── ChunkDocsWorkerTest.java        # 6 tests
    ├── ClusterSummariesWorkerTest.java        # 7 tests
    ├── GenerateWorkerTest.java        # 6 tests
    ├── LeafSummariesWorkerTest.java        # 6 tests
    └── TreeSearchWorkerTest.java        # 7 tests

```
