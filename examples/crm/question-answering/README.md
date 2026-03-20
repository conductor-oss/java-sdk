# Question Answering in Java with Conductor :  Parse, Retrieve, and Generate Answers from a Knowledge Base

A Java Conductor workflow that answers natural language questions from a knowledge base .  parsing the question to extract intent and keywords, retrieving relevant context from the knowledge base, and generating a natural language answer from the retrieved context. Given a `question` and `knowledgeBase`, the pipeline produces parsed intent, relevant context, and a generated answer. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the three-step question answering pipeline.

## Answering Questions with Knowledge-Grounded Responses

An effective question answering system does not just search for keywords .  it understands what the user is asking, finds the most relevant information from a knowledge base, and synthesizes a clear, accurate answer. This requires a pipeline: parse the question to extract its intent and key terms, retrieve the most relevant passages from the knowledge base, and generate an answer that directly addresses the question using the retrieved context.

This workflow implements that three-step pipeline. The question parser analyzes the input to extract intent (e.g., "how-to" vs. "definition" vs. "comparison") and key terms. The context retriever searches the specified knowledge base using the parsed question and returns the most relevant passages. The answer generator synthesizes a natural language response from the retrieved context, directly answering the user's question.

## The Solution

**You just write the question-parsing, context-retrieval, and answer-generation workers. Conductor handles the QA pipeline and passage routing.**

Three workers form the QA pipeline .  question parsing, context retrieval, and answer generation. The parser extracts intent and keywords from the question. The retriever searches the knowledge base for relevant passages. The generator produces a natural language answer from the retrieved context. Conductor sequences the three steps and passes parsed questions, retrieved context, and generated answers between them via JSONPath.

### What You Write: Workers

ParseQuestionWorker extracts intent and keywords, RetrieveContextWorker finds the most relevant passages from the knowledge base, and GenerateAnswerWorker synthesizes a natural language response with a confidence score.

| Worker | Task | What It Does |
|---|---|---|
| **GenerateAnswerWorker** | `qas_generate_answer` | Synthesizes a natural-language answer from retrieved context passages, with a confidence score. |
| **ParseQuestionWorker** | `qas_parse_question` | Analyzes the input question to extract intent type (how-to, definition, comparison) and keywords. |
| **RetrieveContextWorker** | `qas_retrieve_context` | Searches the knowledge base for the most relevant passages matching the parsed question. |

Workers simulate CRM operations .  lead scoring, contact enrichment, deal updates ,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
qas_parse_question
    │
    ▼
qas_retrieve_context
    │
    ▼
qas_generate_answer
```

## Example Output

```
=== Example 637: Question Answering ===

Step 1: Registering task definitions...
  Registered: qas_parse_question, qas_retrieve_context, qas_generate_answer

Step 2: Registering workflow 'qas_question_answering'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [answer] Answer generated (
  [parse] Question type:
  [retrieve] Retrieved 3 relevant passages from

  Status: COMPLETED
  Output: {answer=..., confidence=..., model=..., parsed=...}

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
java -jar target/question-answering-1.0.0.jar
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

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/question-answering-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow qas_question_answering \
  --version 1 \
  --input '{"question": "sample-question", "How do I configure workflow timeouts?": "2025-01-15T10:00:00Z", "knowledgeBase": "sample-knowledgeBase"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w qas_question_answering -s COMPLETED -c 5
```

## How to Extend

Each worker handles one QA step .  connect your search engine (Elasticsearch, Algolia) for passage retrieval and your LLM (Claude, GPT-4) for answer synthesis, and the question-answering workflow stays the same.

- **GenerateAnswerWorker** (`qas_generate_answer`): swap in an LLM (GPT-4, Claude) for real answer generation from retrieved context
- **ParseQuestionWorker** (`qas_parse_question`): use NLP models for intent classification and keyword extraction
- **RetrieveContextWorker** (`qas_retrieve_context`): connect to a vector database (Pinecone, Weaviate) or search engine (Elasticsearch) for semantic retrieval

Swap in a real retrieval engine and LLM and the parse-retrieve-generate QA pipeline continues to answer questions as designed.

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
question-answering/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/questionanswering/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── QuestionAnsweringExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── GenerateAnswerWorker.java
│       ├── ParseQuestionWorker.java
│       └── RetrieveContextWorker.java
└── src/test/java/questionanswering/workers/
    ├── GenerateAnswerWorkerTest.java        # 2 tests
    ├── ParseQuestionWorkerTest.java        # 2 tests
    └── RetrieveContextWorkerTest.java        # 2 tests
```
