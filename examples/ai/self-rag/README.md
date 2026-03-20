# Self-RAG in Java Using Conductor :  Self-Reflective Retrieval with Hallucination and Usefulness Grading

A Java Conductor workflow that implements Self-RAG .  a pipeline that retrieves documents, grades them for relevance, generates an answer, then self-reflects by grading the answer for hallucination (is it supported by the context?) and usefulness (does it address the question?). If the answer fails either quality check, the workflow routes to a refinement step instead of serving the answer. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate retrieval, document grading, generation, self-reflection grading, and conditional routing as independent workers ,  you write the grading logic, Conductor handles the quality-gated routing, retries, durability, and observability for free.

## A RAG Pipeline That Checks Its Own Work

Standard RAG generates and serves .  no self-reflection. Self-RAG adds three grading steps: after retrieval, grade documents for relevance. After generation, grade the answer for hallucination (does it go beyond what the context supports?) and usefulness (does it actually answer the question?). If the answer passes both checks, format and return it. If it fails, route to a refinement step that retries with adjusted parameters.

This creates a conditional pipeline with a quality gate after generation: retrieve, grade docs, generate, grade hallucination, grade usefulness, then branch .  format output on success, refine and retry on failure.

## The Solution

**You write the retrieval grading, hallucination detection, and usefulness scoring logic. Conductor handles the self-reflective routing, retries, and observability.**

Each grading step is an independent worker .  document grading, hallucination grading, usefulness grading. Conductor's `SWITCH` task routes to either the output formatter or the refinement step based on quality scores. Every execution records all grading scores, making it easy to see which questions trigger refinement and why.

### What You Write: Workers

Seven workers implement the self-reflective pipeline .  retrieval, document grading, generation, hallucination grading, usefulness grading, output formatting on pass, and query refinement on fail ,  with a SWITCH gate after the quality checks.

| Worker | Task | What It Does |
|---|---|---|
| **FormatOutputWorker** | `sr_format_output` | Formats the final output when quality gate passes. Returns {answer, sourceCount}. |
| **GenerateWorker** | `sr_generate` | Generates an answer from relevant documents. Returns a fixed answer about Conductor task types. |
| **GradeDocsWorker** | `sr_grade_docs` | Grades retrieved documents for relevance. Filters documents with score >= 0.5. Returns {relevantDocs, filteredCount}. |
| **GradeHallucinationWorker** | `sr_grade_hallucination` | Grades the generated answer for hallucination against source docs. Returns {score: 0.92, grounded: true}. |
| **GradeUsefulnessWorker** | `sr_grade_usefulness` | Grades the generated answer for usefulness: score = 0.88. If score >= 0.7 AND halScore >= 0.7, verdict = "pass". Retu... |
| **RefineRetryWorker** | `sr_refine_retry` | Refines the query when quality gate fails. Returns {refinedQuery}. |
| **RetrieveWorker** | `sr_retrieve` | Retrieves documents relevant to a question. Returns 4 docs: 3 relevant (score >= 0.5) and 1 irrelevant (0.22). |

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
sr_retrieve
    │
    ▼
sr_grade_docs
    │
    ▼
sr_generate
    │
    ▼
sr_grade_hallucination
    │
    ▼
sr_grade_usefulness
    │
    ▼
SWITCH (switch_ref)
    ├── pass: sr_format_output
    └── default: sr_refine_retry
```

## Example Output

```
=== Example 149: Self-RAG ===

Step 1: Registering task definitions...
  Registered: sr_retrieve, sr_grade_docs, sr_generate, sr_grade_hallucination, sr_grade_usefulness, sr_format_output, sr_refine_retry

Step 2: Registering workflow 'self_rag'...
  Workflow registered.

Step 3: Starting workers...
  7 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [format] Packaging final response...
  [generate] Calling OpenAI to produce answer from
  [grade-docs]
  [grade-hallucination] Checking answer against source docs...
  [grade-usefulness] Score:
  [retry] Quality too low - would re-retrieve with refined query
  [retrieve] [SIMULATED] Searching for: \"" + question + "\"

  Status: COMPLETED
  Output: {answer=..., sourceCount=..., mode=..., errorBody=...}

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
java -jar target/self-rag-1.0.0.jar
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
| `CONDUCTOR_OPENAI_API_KEY` | _(not set)_ | OpenAI API key. When set, the generate worker calls OpenAI (gpt-4o-mini). When unset, all workers use simulated output. |

### Live vs Simulated Mode

- **Without `CONDUCTOR_OPENAI_API_KEY`**: All workers return deterministic simulated output (default behavior, no API calls).
- **With `CONDUCTOR_OPENAI_API_KEY`**: GenerateWorker calls OpenAI. Search/retrieve workers (RetrieveWorker) and grading workers remain simulated because they require a real vector database.
- If an OpenAI call fails, the worker automatically falls back to simulated output.

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/self-rag-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow self_rag \
  --version 1 \
  --input '{"question": "sample-question"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w self_rag -s COMPLETED -c 5
```

## How to Extend

Each worker handles one self-reflection step .  swap in LLM-based graders for hallucination and usefulness checks, add refinement logic for failed answers, and the retrieve-grade-generate-reflect pipeline runs unchanged.

- **RetrieveWorker** (`sr_retrieve`): query a vector database (Pinecone, Weaviate, pgvector) to retrieve candidate documents for the question
- **GradeDocsWorker** (`sr_grade_docs`): use an LLM-as-judge or a relevance classifier to filter retrieved documents, keeping only those above a relevance threshold
- **GenerateWorker** (`sr_generate`): call an LLM (GPT-4, Claude) with the relevant documents as context to generate an answer
- **GradeHallucinationWorker** (`sr_grade_hallucination`): use an LLM-as-judge or NLI model to check whether the generated answer is grounded in the source documents
- **GradeUsefulnessWorker** (`sr_grade_usefulness`): use an LLM-as-judge to evaluate whether the answer is useful and addresses the question, combining with hallucination score for a pass/fail verdict
- **RefineRetryWorker** (`sr_refine_retry`): reformulate the query to improve retrieval quality when the quality gate fails, using an LLM to generate a more specific or alternative query
- **FormatOutputWorker** (`sr_format_output`): format the verified answer with source attribution and confidence metadata for the final response

Each grading worker returns the same score/verdict shape, so tightening quality thresholds, swapping grading models, or adding new reflection dimensions requires no workflow changes.

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
self-rag/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/selfrag/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SelfRagExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FormatOutputWorker.java
│       ├── GenerateWorker.java
│       ├── GradeDocsWorker.java
│       ├── GradeHallucinationWorker.java
│       ├── GradeUsefulnessWorker.java
│       ├── RefineRetryWorker.java
│       └── RetrieveWorker.java
└── src/test/java/selfrag/workers/
    ├── FormatOutputWorkerTest.java        # 3 tests
    ├── GenerateWorkerTest.java        # 3 tests
    ├── GradeDocsWorkerTest.java        # 4 tests
    ├── GradeHallucinationWorkerTest.java        # 3 tests
    ├── GradeUsefulnessWorkerTest.java        # 5 tests
    ├── RefineRetryWorkerTest.java        # 3 tests
    └── RetrieveWorkerTest.java        # 4 tests
```
