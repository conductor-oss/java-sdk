# Corrective RAG in Java Using Conductor: Self-Healing Retrieval with Web Search Fallback

Your vector store retrieves three documents for a question, but two are about a completely different topic. Stale embeddings from last quarter's data dump. The LLM cheerfully generates an answer grounded in noise, and your users lose trust because the system sounds confident while being wrong. Standard RAG has no quality gate; it generates from whatever comes back, relevant or not. This example builds a self-healing corrective RAG pipeline using [Conductor](https://github.com/conductor-oss/conductor) that grades retrieved documents for relevance and automatically falls back to web search when the vector store misses the mark.

## When Your Vector Store Doesn't Have the Answer

Standard RAG pipelines retrieve documents and generate.; no questions asked. If the vector store returns irrelevant content (stale embeddings, topic drift, missing coverage), the LLM hallucinates confidently from bad context. Users get wrong answers with no indication that the retrieval failed.

Corrective RAG adds a quality gate: after retrieval, an LLM-based grader scores each document for relevance. If the average relevance score falls below a threshold, the pipeline abandons the vector store results entirely and falls back to web search. The answer is then generated from fresh web results instead.

This creates a branching decision. Retrieve, grade, then either generate from the original documents or pivot to web search and generate from those results. Without orchestration, you'd implement this as nested if/else blocks with separate error handling for each path, no visibility into which branch was taken, and no easy way to retry a failed web search without re-running the entire pipeline.

## The Solution

**You write the retrieval grading and web search fallback logic. Conductor handles the conditional routing, retries, and observability.**

Each stage is an independent worker. Retrieving documents, grading relevance, searching the web, generating answers. Conductor's `SWITCH` task inspects the grader's verdict and routes to the right generation path. If the web search times out, Conductor retries it. Every execution records which path was taken and why, so you can audit retrieval quality over time without adding logging code.

### What You Write: Workers

Five workers split the self-healing pipeline across retrieval, relevance grading, web search fallback, and two generation paths, the SWITCH task decides which generation path runs based on the grader's verdict.

| Worker | Task | What It Does | Real / Notes |
|---|---|---|---|
| **RetrieveDocsWorker** | `cr_retrieve_docs` | Retrieves 3 documents from the vector store with LOW relevance scores (0.15-0.25, producing low scores for off-topic queries) to demonstrate the corrective fallback path | Requires API key, or swap in Pinecone, Weaviate, Qdrant, or pgvector |
| **GradeRelevanceWorker** | `cr_grade_relevance` | Scores each retrieved document for relevance (0-1 scale), computes the average, and returns a verdict: `"relevant"` if avg >= 0.5, `"irrelevant"` otherwise | Requires API key, or swap in an LLM-based grader (Claude, GPT-4) |
| **GenerateAnswerWorker** | `cr_generate_answer` | Generates a grounded answer from the vector store documents (taken when the verdict is `"relevant"`) | Requires API key, or swap in Claude Messages API or OpenAI Chat Completions |
| **WebSearchWorker** | `cr_web_search` | Performs a web search fallback when retrieved documents are irrelevant, returning 3 web results with title and snippet | Requires API key, or swap in Tavily, Brave Search, SerpAPI, or Google Custom Search |
| **GenerateFromWebWorker** | `cr_generate_from_web` | Generates a grounded answer from web search results (taken when the verdict is `"irrelevant"`) | Requires API key, or swap in Claude Messages API or OpenAI Chat Completions |

GenerateAnswerWorker and GenerateFromWebWorker require CONDUCTOR_OPENAI_API_KEY. RetrieveDocsWorker uses Jaccard similarity over bundled docs. WebSearchWorker fetches results from the Wikipedia API.

### The Workflow

```
cr_retrieve_docs
    │
    ▼
cr_grade_relevance
    │
    ▼
SWITCH (switch_ref)
    ├── relevant: cr_generate_answer
    └── default: cr_web_search -> cr_generate_from_web

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
java -jar target/corrective-rag-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(required)_ | OpenAI API key. When set, generate workers call OpenAI (gpt-4o-mini). Workers throw IllegalStateException if not set. |

### API Key Requirement

All LLM workers (GenerateAnswerWorker, GenerateFromWebWorker) require CONDUCTOR_OPENAI_API_KEY and throw IllegalStateException if missing.

RetrieveDocsWorker uses Jaccard similarity over bundled technical documents. WebSearchWorker fetches real results from the Wikipedia search API. GradeRelevanceWorker uses algorithmic relevance scoring.

## Example Output

```
=== Corrective RAG Demo: Self-Healing Retrieval ===

Step 1: Registering task definitions...
  Registered: cr_retrieve_docs, cr_grade_relevance, cr_generate_answer, cr_web_search, cr_generate_from_web

Step 2: Registering workflow 'corrective_rag'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  [retrieve] Retrieved 3 documents
  [grade] Grading 3 documents for relevance...
  [grade] Avg relevance: 0.20 -> verdict: irrelevant
  [web_search] Searching the web for: "What is Conductor?"
  [generate_web] Generating answer from 3 web results

  Workflow ID: f2a3b4c5-...

Step 5: Waiting for completion...
  Status: COMPLETED
  Verdict: irrelevant (avg relevance: 0.20)
  Source: webSearch

--- Corrective RAG Pattern ---
  Vector store returned irrelevant results (avg score 0.20 < 0.50 threshold).
  Automatically fell back to web search for fresh, relevant results.

Result: PASSED

```

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/corrective-rag-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow corrective_rag \
  --version 1 \
  --input '{"question": "What are the benefits of workflow orchestration?"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w corrective_rag -s COMPLETED -c 5

```

## The Corrective RAG Pipeline

Standard RAG blindly generates from whatever the vector store returns. Corrective RAG adds a quality gate:

1. **Retrieve** (`cr_retrieve_docs`): Query the vector store for documents related to the question. The documents come back with relevance scores.

2. **Grade** (`cr_grade_relevance`): An LLM-based grader scores each document for relevance to the question on a 0-1 scale. If the average score is >= 0.5, the verdict is `"relevant"`; otherwise `"irrelevant"`.

3. **Route** (SWITCH): Conductor's SWITCH task inspects the verdict and routes to the appropriate generation path:
   - **Relevant**: Generate the answer directly from the retrieved documents
   - **Irrelevant**: Fall back to web search, then generate from fresh web results

This self-healing pattern ensures the user gets a grounded answer even when the vector store has stale embeddings, topic drift, or missing coverage. Every execution records which path was taken and the average relevance score, so you can audit retrieval quality over time.

## How to Extend

Each worker handles one stage of the self-healing pipeline. Swap in a real vector store, an LLM-based relevance grader, and a web search API like Tavily or SerpAPI, and the corrective routing runs unchanged.

- **RetrieveDocsWorker** (`cr_retrieve_docs`): swap in a real vector store query against Pinecone, Weaviate, pgvector, or Elasticsearch
- **GradeRelevanceWorker** (`cr_grade_relevance`): replace with an LLM call (Claude, GPT-4) that scores document-question relevance on a 0-1 scale, or use a fine-tuned cross-encoder model for faster grading
- **WebSearchWorker** (`cr_web_search`): integrate a real search API (Tavily, Brave Search, SerpAPI, or Google Custom Search)
- **GenerateAnswerWorker** / **GenerateFromWebWorker**. Swap in Claude Messages API, OpenAI Chat Completions, or Ollama for grounded answer generation
- **Add a confidence threshold**: make the 0.5 relevance threshold configurable as a workflow input parameter, so you can tune the sensitivity per use case

Each worker preserves the same output contract. Swap the grading model, the vector store, or the web search API, and the corrective routing logic stays untouched.

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
corrective-rag/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/correctiverag/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CorrectiveRagExample.java    # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GenerateAnswerWorker.java    # Generate from vector store context
│       ├── GenerateFromWebWorker.java   # Generate from web search results
│       ├── GradeRelevanceWorker.java    # Score documents, verdict: relevant/irrelevant
│       ├── RetrieveDocsWorker.java      # Vector store retrieval (simulates low relevance)
│       └── WebSearchWorker.java         # Web search fallback
└── src/test/java/correctiverag/workers/
    ├── GenerateAnswerWorkerTest.java
    ├── GenerateFromWebWorkerTest.java
    ├── GradeRelevanceWorkerTest.java
    ├── RetrieveDocsWorkerTest.java
    └── WebSearchWorkerTest.java

```
