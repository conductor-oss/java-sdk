# RAG Evaluation in Java Using Conductor :  Faithfulness, Relevance, and Coherence Scoring in Parallel

A Java Conductor workflow that runs a RAG pipeline and then evaluates the output on three quality dimensions simultaneously .  faithfulness (does the answer stick to the retrieved context?), relevance (does it address the question?), and coherence (is it well-structured and readable?). Conductor's `FORK_JOIN` runs all three evaluations in parallel, then aggregates the scores. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate RAG execution, parallel evaluation, and score aggregation as independent workers ,  you write the evaluation logic, Conductor handles parallelism, retries, durability, and observability for free.

## Measuring RAG Quality Systematically

A RAG pipeline can produce answers that are relevant but unfaithful (the answer sounds right but isn't supported by the context), faithful but incoherent (accurate but poorly structured), or coherent but irrelevant (well-written but doesn't address the question). You need all three metrics to assess quality.

Running evaluations sequentially triples the time. Running them in parallel requires thread management and synchronization. And without tracking scores over time, you can't tell whether a prompt change improved faithfulness at the cost of coherence.

## The Solution

**You write the faithfulness, relevance, and coherence scoring logic. Conductor handles the parallel evaluation, retries, and observability.**

The RAG pipeline runs first, producing a question, context, and answer. Then Conductor's `FORK_JOIN` evaluates faithfulness, relevance, and coherence in parallel. An aggregation worker combines the scores into an overall quality rating. Every evaluation is tracked, building a dataset for RAG quality monitoring over time.

### What You Write: Workers

Five workers evaluate RAG quality .  running the RAG pipeline, then scoring faithfulness, relevance, and coherence in parallel via FORK_JOIN, and aggregating the three scores into a unified quality report.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateScoresWorker** | `re_aggregate_scores` | Worker that aggregates evaluation scores from faithfulness, relevance, and coherence. Computes an overall average sco... |
| **EvalCoherenceWorker** | `re_eval_coherence` | Worker that evaluates the coherence of a RAG answer. Checks whether the answer is logically structured and well-organ... |
| **EvalFaithfulnessWorker** | `re_eval_faithfulness` | Worker that evaluates the faithfulness of a RAG answer. Checks whether the answer is supported by the retrieved context. |
| **EvalRelevanceWorker** | `re_eval_relevance` | Worker that evaluates the relevance of a RAG answer. Checks whether the answer addresses the original question. |
| **RunRagWorker** | `re_run_rag` | Worker that simulates running a RAG pipeline. Takes a question and returns an answer, context passages, and retrieved... |

Workers simulate LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode .  the workflow and worker interfaces stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Parallel execution** | FORK_JOIN runs multiple tasks simultaneously and waits for all to complete |

### The Workflow

```
re_run_rag
    │
    ▼
FORK_JOIN
    ├── re_eval_faithfulness
    ├── re_eval_relevance
    └── re_eval_coherence
    │
    ▼
JOIN (wait for all branches)
re_aggregate_scores
```

## Example Output

```
=== Example 162: RAG Evaluation Pipeline ===

Step 1: Registering task definitions...
  Registered: re_run_rag, re_eval_faithfulness, re_eval_relevance, re_eval_coherence, re_aggregate_scores

Step 2: Registering workflow 'rag_evaluation_pipeline'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [aggregate] overallScore=
  [coherence] score=
  [faithfulness] score=
  [relevance] score=
  [run_rag] Processed question:

  Status: COMPLETED
  Output: {score=..., reason=..., faithfulness=..., relevance=...}

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
java -jar target/rag-evaluation-1.0.0.jar
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
java -jar target/rag-evaluation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rag_evaluation_pipeline \
  --version 1 \
  --input '{}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rag_evaluation_pipeline -s COMPLETED -c 5
```

## How to Extend

Each worker scores one quality dimension .  swap in LLM-based evaluators for faithfulness, relevance, and coherence checks, and the parallel evaluation-aggregation pipeline runs unchanged.

- **RunRagWorker** (`re_run_rag`): call your production RAG pipeline (retrieve + generate) and capture the answer, retrieved context, and metadata for evaluation
- **EvalFaithfulnessWorker** (`re_eval_faithfulness`): use an LLM-as-judge (GPT-4, Claude) or DeepEval/RAGAS to score whether the answer is supported by the retrieved context
- **EvalRelevanceWorker** (`re_eval_relevance`): use an LLM-as-judge or BERTScore to measure whether the answer addresses the original question
- **EvalCoherenceWorker** (`re_eval_coherence`): use an LLM-as-judge to evaluate logical structure, consistency, and readability of the generated answer
- **AggregateScoresWorker** (`re_aggregate_scores`): compute weighted overall scores with configurable thresholds, and push results to experiment tracking (MLflow, Weights & Biases) for trend monitoring

Each evaluation worker returns the same score/reasoning shape, so adding new quality dimensions (e.g., completeness, conciseness) requires only a new worker and fork branch.

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
rag-evaluation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragevaluation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagEvaluationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateScoresWorker.java
│       ├── EvalCoherenceWorker.java
│       ├── EvalFaithfulnessWorker.java
│       ├── EvalRelevanceWorker.java
│       └── RunRagWorker.java
└── src/test/java/ragevaluation/workers/
    ├── AggregateScoresWorkerTest.java        # 6 tests
    ├── EvalCoherenceWorkerTest.java        # 3 tests
    ├── EvalFaithfulnessWorkerTest.java        # 3 tests
    ├── EvalRelevanceWorkerTest.java        # 3 tests
    └── RunRagWorkerTest.java        # 4 tests
```
