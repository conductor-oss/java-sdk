# Web Scraping RAG in Java Using Conductor :  Scrape Pages, Index Content, Answer Questions

A Java Conductor workflow that scrapes web pages from a list of URLs, chunks the extracted content, embeds and stores the vectors, then runs a RAG query against the freshly scraped content. This is a complete pipeline from raw web pages to answered questions .  ingestion and retrieval in a single workflow. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate scraping, chunking, embedding/storage, querying, and generation as independent workers ,  you write the scraping and RAG logic, Conductor handles sequencing, retries, durability, and observability for free.

## RAG Over Live Web Content

Sometimes the knowledge you need isn't in a pre-indexed corpus .  it's on web pages that change frequently. Product documentation, competitor websites, news articles, and regulatory filings need to be scraped, indexed, and queried on demand. This pipeline scrapes the content, processes it into searchable vectors, and immediately queries it ,  all in a single workflow execution.

Each step can fail independently: a URL might return a 403, the HTML might have unexpected structure, the embedding API might rate-limit, or the vector store might be temporarily unavailable. Without orchestration, a single scraping error means restarting the entire pipeline.

## The Solution

**You write the scraping, chunking, and indexing logic. Conductor handles the end-to-end ingestion-to-query pipeline, retries, and observability.**

Each stage is an independent worker .  web scraping (fetching and parsing HTML from multiple URLs), content chunking, embedding and vector storage, query embedding, and answer generation. Conductor sequences the full pipeline, retries failed scrapes individually, and tracks every execution from URLs through final answer.

### What You Write: Workers

Five workers form the scrape-to-answer pipeline .  fetching web pages, chunking the extracted content, embedding and storing vectors, querying the freshly indexed content, and generating an answer from the scraped knowledge.

| Worker | Task | What It Does |
|---|---|---|
| **ChunkWorker** | `wsrag_chunk` | Worker that chunks scraped page content into sentence pairs. Splits each page's text by ". " and groups sentences in ... |
| **EmbedStoreWorker** | `wsrag_embed_store` | Worker that embeds chunks and stores them in a vector database. Returns the stored chunk IDs and count. |
| **GenerateWorker** | `wsrag_generate` | Worker that generates a final answer from the question and retrieved context. |
| **QueryWorker** | `wsrag_query` | Worker that queries the vector store with a question. Returns fixed relevant context chunks with similarity scores. |
| **ScrapeWorker** | `wsrag_scrape` | Worker that scrapes web pages and extracts content. Returns fixed demo pages with title, text, and word count. |

Workers simulate LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode .  the workflow and worker interfaces stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
wsrag_scrape
    │
    ▼
wsrag_chunk
    │
    ▼
wsrag_embed_store
    │
    ▼
wsrag_query
    │
    ▼
wsrag_generate
```

## Example Output

```
=== Example 142: Web Scraping to RAG ===

Step 1: Registering task definitions...
  Registered: wsrag_scrape, wsrag_chunk, wsrag_embed_store, wsrag_query, wsrag_generate

Step 2: Registering workflow 'web_scraping_rag_workflow'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [chunk] Created
  [embed] Generated
  [generate] Response from OpenAI (LIVE)
  [query] Searching for: \"" + question + "\"
  [scrape] Fetching

  Status: COMPLETED
  Output: {chunks=..., chunkCount=..., storedIds=..., count=...}

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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/web-scraping-rag-1.0.0.jar
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
java -jar target/web-scraping-rag-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow web_scraping_rag_workflow \
  --version 1 \
  --input '{"urls": "https://api.example.com/resource", "https://docs.example.com/overview": "sample-https://docs.example.com/overview", "question": "sample-question"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w web_scraping_rag_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one scraping or RAG step .  swap in Jsoup or Playwright for web scraping, connect a real embedding API and vector store for indexing, and the scrape-chunk-embed-query-generate pipeline runs unchanged.

- **ScrapeWorker** (`wsrag_scrape`): use Jsoup, Playwright, or Selenium to scrape web pages, extracting clean text content with metadata (title, URL, timestamp)
- **ChunkWorker** (`wsrag_chunk`): split scraped content into semantic chunks using sentence boundary detection or LangChain text splitters with configurable overlap
- **EmbedStoreWorker** (`wsrag_embed_store`): embed chunks using an embedding API (OpenAI, Cohere) and store them in a vector database (Pinecone, Weaviate, pgvector) with source URL metadata
- **QueryWorker** (`wsrag_query`): query the vector store with the user's question and retrieve the most relevant web content chunks with similarity scores
- **GenerateWorker** (`wsrag_generate`): send retrieved web content as context to an LLM (GPT-4, Claude) to generate an answer with source URL attribution

Each worker preserves its content/embedding contract, so swapping Jsoup for Playwright, upgrading the embedding model, or switching vector stores requires no workflow changes.

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
web-scraping-rag/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/webscrapingrag/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── WebScrapingRagExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ChunkWorker.java
│       ├── EmbedStoreWorker.java
│       ├── GenerateWorker.java
│       ├── QueryWorker.java
│       └── ScrapeWorker.java
└── src/test/java/webscrapingrag/workers/
    ├── ChunkWorkerTest.java        # 5 tests
    ├── EmbedStoreWorkerTest.java        # 4 tests
    ├── GenerateWorkerTest.java        # 5 tests
    ├── QueryWorkerTest.java        # 4 tests
    └── ScrapeWorkerTest.java        # 5 tests
```
