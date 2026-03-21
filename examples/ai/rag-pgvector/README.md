# RAG with pgvector in Java Using Conductor :  Vector Search in PostgreSQL

A Java Conductor workflow that implements RAG using pgvector. the PostgreSQL extension that adds vector similarity search to your existing Postgres database. The pipeline embeds the question, queries a pgvector-enabled table using cosine distance or inner product operators, and generates an answer from the retrieved rows. If you already run PostgreSQL, pgvector gives you vector search without adding a separate database. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate embedding, pgvector queries, and generation as independent workers,  you write the SQL and embedding logic, Conductor handles sequencing, retries, durability, and observability.

## RAG Without a Separate Vector Database

pgvector turns any PostgreSQL table into a vector store. You add a `vector(1536)` column to your existing table, create an ivfflat or HNSW index, and query with `ORDER BY embedding <=> $1 LIMIT 10`. Your documents, metadata, and vectors live in the same database. no data synchronization between a document store and a vector store.

The RAG pipeline embeds the question, runs the pgvector similarity query, and generates from the results. Each step can fail independently, and you want the Postgres query retried during temporary connection issues without re-embedding.

## The Solution

**You write the embedding and pgvector SQL query logic. Conductor handles the RAG pipeline, retries, and observability.**

Each stage is an independent worker. question embedding, pgvector SQL query (specifying the table), and answer generation. Conductor sequences them, retries the database query during connection blips, and tracks every search with the question, retrieved rows, and generated answer.

### What You Write: Workers

Three workers integrate pgvector into the RAG pipeline. embedding the query, running SQL-based vector similarity queries against a PostgreSQL table with the pgvector extension, and generating an answer from the results.

| Worker | Task | What It Does |
|---|---|---|
| **PgvecEmbedWorker** | `pgvec_embed` | Worker that encodes a question into a fixed embedding vector for pgvector. In production this would call an embedding... |
| **PgvecGenerateWorker** | `pgvec_generate` | Worker that generates an answer from a question and retrieved pgvector rows. In production this would call an LLM (e.... |
| **PgvecQueryWorker** | `pgvec_query` | Worker that builds a pgvector SQL query and returns simulated result rows. Takes embedding, table name, limit, and di... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
pgvec_embed
    │
    ▼
pgvec_query
    │
    ▼
pgvec_generate

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
java -jar target/rag-pgvector-1.0.0.jar

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
java -jar target/rag-pgvector-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rag_pgvector_workflow \
  --version 1 \
  --input '{"question": "What is workflow orchestration?", "table": "sample-table"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rag_pgvector_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one RAG stage. swap in a real embedding API, query your PostgreSQL table with pgvector's cosine distance operator, and the embed-search-generate pipeline runs unchanged.

- **PgvecEmbedWorker** (`pgvec_embed`): call an embedding API (OpenAI text-embedding-3-small, Cohere embed-english-v3) to vectorize the question for pgvector search
- **PgvecGenerateWorker** (`pgvec_generate`): send pgvector query results as context to an LLM (OpenAI GPT-4, Anthropic Claude) to generate a grounded answer
- **PgvecQueryWorker** (`pgvec_query`): execute real pgvector SQL queries against PostgreSQL using the appropriate distance operator (`<=>` cosine, `<->` L2, `<#>` inner product) with JDBC

The embed/query/generate contract stays fixed. switch between cosine and inner product distance, add WHERE clause filters, or swap embedding models without modifying the workflow.

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
rag-pgvector/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragpgvector/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagPgvectorExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PgvecEmbedWorker.java
│       ├── PgvecGenerateWorker.java
│       └── PgvecQueryWorker.java
└── src/test/java/ragpgvector/workers/
    ├── PgvecEmbedWorkerTest.java        # 5 tests
    ├── PgvecGenerateWorkerTest.java        # 5 tests
    └── PgvecQueryWorkerTest.java        # 7 tests

```
