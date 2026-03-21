# Code RAG in Java Using Conductor :  Language-Aware Code Search and Answer Generation

A Java Conductor workflow that implements RAG specifically for code. parsing the natural language question to identify programming language and intent, embedding the query with code-aware representations, searching a code index for relevant functions/classes/snippets, and generating a code-grounded answer. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate query parsing, code embedding, index search, and generation as independent workers,  you write the code search logic, Conductor handles sequencing, retries, durability, and observability.

## Code Search Is Not Text Search

Searching code requires different strategies than searching documents. A question like "How do I sort a list in Python?" needs language-aware parsing (extract "sort", "list", "Python"), code-specific embeddings (that understand function signatures, not just prose), and an index that stores code snippets with metadata like language, framework, and purpose. Standard text-based RAG misses semantic code structures.

The pipeline has four steps: parse the natural language query to extract language and intent, generate a code-aware embedding, search a code-specific index, and generate an answer with code examples from the retrieved snippets.

## The Solution

**You write the query parsing, code-aware embedding, and code index search logic. Conductor handles the pipeline, retries, and observability.**

Each stage is an independent worker. query parsing (extracting language and intent), code-aware embedding, code index search, and answer generation with code snippets. Conductor sequences them, retries the search if the index is temporarily unavailable, and tracks every query with the parsed intent, retrieved code snippets, and generated answer.

### What You Write: Workers

Four workers handle code-aware retrieval. parsing the natural language query for language and intent, embedding the code-specific query, searching a code index with language-aware filtering, and generating an answer that includes code snippets with syntax context.

| Worker | Task | What It Does |
|---|---|---|
| **EmbedCodeQueryWorker** | `cr_embed_code_query` | Worker that embeds a parsed code query into a vector representation using OpenAI text-embedding-3-small. |
| **GenerateCodeAnswerWorker** | `cr_generate_code_answer` | Worker that generates a code-aware answer from the question and retrieved code snippets. |
| **ParseQueryWorker** | `cr_parse_query` | Worker that parses a code-related question to extract intent, keywords, and language. Returns a fixed parsed intent o... |
| **SearchCodeIndexWorker** | `cr_search_code_index` | Worker that searches a code index using the embedding vector and code filter. Returns 3 fixed code snippets with id, ... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
cr_parse_query
    │
    ▼
cr_embed_code_query
    │
    ▼
cr_search_code_index
    │
    ▼
cr_generate_code_answer

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
java -jar target/rag-code-1.0.0.jar

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
| `CONDUCTOR_OPENAI_API_KEY` | _(not set)_ | OpenAI API key. When set, EmbedCodeQueryWorker calls text-embedding-3-small and GenerateCodeAnswerWorker calls gpt-4o-mini instead of using simulated output |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/rag-code-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow code_rag_workflow \
  --version 1 \
  --input '{"question": "What is workflow orchestration?", "language": "en"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w code_rag_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker handles one code search concern. swap in code-specific embeddings like CodeBERT or StarCoder, connect a code index with language and framework metadata, and the parse-embed-search-generate pipeline runs unchanged.

- **ParseQueryWorker** (`cr_parse_query`): use an LLM (GPT-4, Claude) or spaCy NER to extract programming intent, language, and keywords from natural-language code questions
- **EmbedCodeQueryWorker** (`cr_embed_code_query`): embed code queries using a code-specialized model (OpenAI Codex embeddings, Voyage Code, or StarCoder embeddings) for better code search relevance
- **SearchCodeIndexWorker** (`cr_search_code_index`): query a code search index (Sourcegraph, GitHub Code Search API, or a custom tree-sitter + vector DB index) to retrieve relevant code snippets with AST context
- **GenerateCodeAnswerWorker** (`cr_generate_code_answer`): call a code-specialized LLM (GPT-4, Claude, or CodeLlama) with retrieved code snippets as context to generate accurate, runnable code answers

Each worker's code-search contract is fixed. add support for new languages, swap the code embedding model, or change the index backend without modifying the workflow.

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
rag-code/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragcode/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagCodeExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EmbedCodeQueryWorker.java
│       ├── GenerateCodeAnswerWorker.java
│       ├── ParseQueryWorker.java
│       └── SearchCodeIndexWorker.java
└── src/test/java/ragcode/workers/
    ├── EmbedCodeQueryWorkerTest.java        # 6 tests
    ├── GenerateCodeAnswerWorkerTest.java        # 6 tests
    ├── ParseQueryWorkerTest.java        # 6 tests
    └── SearchCodeIndexWorkerTest.java        # 6 tests

```
