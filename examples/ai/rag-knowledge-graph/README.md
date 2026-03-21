# Knowledge Graph RAG in Java Using Conductor :  Graph Traversal + Vector Search for Enriched Context

A Java Conductor workflow that combines knowledge graph traversal with vector similarity search. extracting entities from the question, running graph traversal and vector search in parallel, merging the structured (graph) and unstructured (vector) results, and generating an answer from the enriched context. The graph provides explicit relationships ("Company X acquired Company Y in 2023") while the vector store provides relevant passages. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate entity extraction, parallel retrieval, context merging, and generation as independent workers,  you write the graph and search logic, Conductor handles parallelism, retries, durability, and observability.

## When Vector Search Alone Misses Relationships

Vector search finds semantically similar text, but it doesn't understand relationships. A question like "Who are the competitors of Company X's parent company?" requires traversing entity relationships (Company X -> parent company -> competitors) that aren't captured in embedding similarity. Knowledge graph RAG extracts entities from the question, traverses the graph to find related entities and facts, and combines those structured results with vector search results for richer context.

The graph traversal and vector search are independent. they can run in parallel, and their results are complementary (structured facts + relevant passages). After both complete, a merge step combines them into a unified context for generation.

## The Solution

**You write the entity extraction, graph traversal, and vector search logic. Conductor handles the parallel retrieval, retries, and observability.**

Entity extraction runs first. Then Conductor's `FORK_JOIN` runs graph traversal and vector search in parallel. A merge worker combines structured graph results with unstructured vector results, and a generation worker produces the answer from the enriched context.

### What You Write: Workers

Five workers combine graph and vector retrieval. extracting entities from the question, traversing the knowledge graph and searching the vector store in parallel via FORK_JOIN, merging both context types, and generating an answer from the unified graph-plus-vector context.

| Worker | Task | What It Does |
|---|---|---|
| **ExtractEntitiesWorker** | `kg_extract_entities` | Worker that extracts named entities from the user's question. Returns 4 fixed entities with name, type, and id fields... |
| **GenerateWorker** | `kg_generate` | Worker that generates a final answer using the question, merged context from both graph and vector retrieval, and ext... |
| **GraphTraverseWorker** | `kg_graph_traverse` | Worker that traverses a knowledge graph starting from extracted entities. Returns 7 facts (subject/predicate/object/c... |
| **MergeContextWorker** | `kg_merge_context` | Worker that merges context from graph traversal and vector search. Combines graphFacts, graphRelations, and vectorDoc... |
| **VectorSearchWorker** | `kg_vector_search` | Worker that performs vector similarity search using the question and entity hints. Returns 4 documents with id, text,... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
kg_extract_entities
    │
    ▼
FORK_JOIN
    ├── kg_graph_traverse
    └── kg_vector_search
    │
    ▼
JOIN (wait for all branches)
kg_merge_context
    │
    ▼
kg_generate

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
java -jar target/rag-knowledge-graph-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(none)_ | OpenAI API key for embeddings and generation. When absent, workers use demo responses. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/rag-knowledge-graph-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rag_knowledge_graph_workflow \
  --version 1 \
  --input '{"question": "What is workflow orchestration?"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rag_knowledge_graph_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one retrieval source. swap in Neo4j or Amazon Neptune for graph traversal, connect a vector store for semantic search, merge structured facts with relevant passages, and the parallel graph-plus-vector pipeline runs unchanged.

- **ExtractEntitiesWorker** (`kg_extract_entities`): use an NER model (spaCy, Hugging Face NER pipeline) or an LLM (GPT-4, Claude) to extract named entities from the user's question
- **GraphTraverseWorker** (`kg_graph_traverse`): query a graph database (Neo4j via Cypher, Amazon Neptune via Gremlin, or ArangoDB) starting from extracted entities to discover relationships and facts
- **VectorSearchWorker** (`kg_vector_search`): query a vector database (Pinecone, Qdrant, Weaviate) using the question and entity hints for complementary unstructured retrieval
- **MergeContextWorker** (`kg_merge_context`): combine structured graph facts/relations with unstructured vector search results into a unified context with provenance tracking
- **GenerateWorker** (`kg_generate`): send the merged graph + vector context to an LLM (GPT-4, Claude) to generate an answer that leverages both structured relationships and unstructured text

Each retrieval worker returns the same context shape, so upgrading the graph database, swapping vector stores, or refining entity extraction requires no changes to the merge or generation workers.

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
rag-knowledge-graph/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragknowledgegraph/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagKnowledgeGraphExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExtractEntitiesWorker.java
│       ├── GenerateWorker.java
│       ├── GraphTraverseWorker.java
│       ├── MergeContextWorker.java
│       └── VectorSearchWorker.java
└── src/test/java/ragknowledgegraph/workers/
    ├── ExtractEntitiesWorkerTest.java        # 6 tests
    ├── GenerateWorkerTest.java        # 7 tests
    ├── GraphTraverseWorkerTest.java        # 7 tests
    ├── MergeContextWorkerTest.java        # 7 tests
    └── VectorSearchWorkerTest.java        # 7 tests

```
