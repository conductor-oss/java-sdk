# RAG Quality Gates in Java Using Conductor :  Relevance and Faithfulness Checks Before Serving Answers

A Java Conductor workflow that adds two quality gates to a RAG pipeline .  a relevance gate after retrieval (are the documents relevant to the question?) and a faithfulness gate after generation (is the answer supported by the retrieved context?). If either gate fails, the answer is rejected instead of served. Conductor's `SWITCH` tasks implement the gates as conditional routing. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate retrieval, quality checking, generation, and gating as independent workers ,  you write the quality check logic, Conductor handles conditional routing, retries, durability, and observability for free.

## Don't Serve Bad Answers

A standard RAG pipeline retrieves and generates without checking quality. If the retrieved documents are irrelevant (the vector store returned noise), the LLM generates a hallucinated answer from bad context. If the answer contradicts the retrieved documents (the LLM went off-script), the user gets incorrect information. Quality gates catch both: a relevance check rejects bad retrievals before generation, and a faithfulness check rejects unfaithful answers before serving.

This creates a pipeline with two conditional branch points .  retrieve, check relevance (pass or reject), generate, check faithfulness (pass or reject). Without orchestration, this is nested if/else logic with no visibility into which gate rejected which query.

## The Solution

**You write the relevance and faithfulness check logic. Conductor handles the quality-gated routing, retries, and observability.**

Each step is an independent worker .  retrieval, relevance checking, generation, faithfulness checking, rejection. Conductor's `SWITCH` tasks route to rejection when a quality gate fails. Every execution records which gates passed and which rejected, building a dataset for quality monitoring.

### What You Write: Workers

Five workers implement a dual quality gate .  retrieving documents, checking relevance (first SWITCH gate), generating an answer, checking faithfulness (second SWITCH gate), and rejecting answers that fail either check.

| Worker | Task | What It Does |
|---|---|---|
| **CheckFaithfulnessWorker** | `qg_check_faithfulness` | Worker that checks the faithfulness of a generated answer against the source documents. Evaluates 3 fixed claims, com... |
| **CheckRelevanceWorker** | `qg_check_relevance` | Worker that checks the relevance of retrieved documents. Computes the average score across all documents and compares... |
| **GenerateWorker** | `qg_generate` | Worker that generates an answer from the question and retrieved documents. Combines the document texts into a synthes... |
| **RejectWorker** | `qg_reject` | Worker that handles rejection when a quality gate fails. Takes a reason and score, and returns rejected=true along wi... |
| **RetrieveWorker** | `qg_retrieve` | Worker that retrieves documents for the given question. Returns 3 fixed documents with id, text, and relevance score.... |

Workers simulate LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode .  the workflow and worker interfaces stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Conditional routing** | SWITCH tasks route execution to different paths based on worker output |

### The Workflow

```
qg_retrieve
    │
    ▼
qg_check_relevance
    │
    ▼
SWITCH (relevance_gate_ref)
    ├── pass: qg_generate -> qg_check_faithfulness -> faithfulness_gate
    ├── fail: qg_reject
```

## Example Output

```
=== Example 157: RAG Quality Gates ===

Step 1: Registering task definitions...
  Registered: qg_retrieve, qg_check_relevance, qg_generate, qg_check_faithfulness, qg_reject

Step 2: Registering workflow 'rag_quality_gates_workflow'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [check_faithfulness] Checking faithfulness of generated answer
  [check_relevance] Average relevance:
  [generate] Generating answer for:
  [reject] Rejecting: reason=
  [retrieve] Retrieving documents for:

  Status: COMPLETED
  Output: {faithfulnessScore=..., claims=..., threshold=..., decision=...}

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
java -jar target/rag-quality-gates-1.0.0.jar
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
java -jar target/rag-quality-gates-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rag_quality_gates_workflow \
  --version 1 \
  --input '{"question": "sample-question"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rag_quality_gates_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one quality gate .  swap in LLM-based relevance scoring after retrieval, add faithfulness verification after generation, and the gated serve-or-reject pipeline runs unchanged.

- **RetrieveWorker** (`qg_retrieve`): query a vector database (Pinecone, Weaviate, pgvector) to retrieve relevant documents for the question
- **GenerateWorker** (`qg_generate`): call an LLM (OpenAI GPT-4, Anthropic Claude) with retrieved context to generate an answer
- **CheckRelevanceWorker** (`qg_check_relevance`): use an LLM-as-judge or BERTScore to evaluate whether retrieved documents are relevant to the question, enforcing a configurable threshold
- **CheckFaithfulnessWorker** (`qg_check_faithfulness`): use an LLM-as-judge or NLI model to verify each claim in the answer is supported by the source documents
- **RejectWorker** (`qg_reject`): return a structured rejection response with the failing gate, score, and threshold so the caller knows exactly why the answer was rejected

Each quality gate worker returns the same pass/fail verdict shape, so tightening thresholds, adding new quality dimensions, or swapping the grading model requires no workflow changes.

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
rag-quality-gates/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragqualitygates/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagQualityGatesExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckFaithfulnessWorker.java
│       ├── CheckRelevanceWorker.java
│       ├── GenerateWorker.java
│       ├── RejectWorker.java
│       └── RetrieveWorker.java
└── src/test/java/ragqualitygates/workers/
    ├── CheckFaithfulnessWorkerTest.java        # 6 tests
    ├── CheckRelevanceWorkerTest.java        # 6 tests
    ├── GenerateWorkerTest.java        # 5 tests
    ├── RejectWorkerTest.java        # 6 tests
    └── RetrieveWorkerTest.java        # 5 tests
```
