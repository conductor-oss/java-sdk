# RAG Citation in Java Using Conductor: Generate Answers with Source Citations and Verification

Your RAG system gives a great answer, but when the VP asks "where did you get that number?" you can't point to a source. Worse, the LLM peppered the response with "[1]" and "[2]" citations that reference documents it never actually retrieved. fabricated footnotes that look authoritative but lead nowhere. This example builds a citation-verified RAG pipeline using [Conductor](https://github.com/conductor-oss/conductor), generating answers with inline source markers, extracting every citation from the text, and cross-referencing each one against the documents that were actually retrieved, so every claim traces back to a real source or gets flagged.

## Trust But Verify: Citations in RAG Answers

LLMs can claim to cite sources while actually hallucinating references. A standard RAG pipeline provides documents as context, but the generated answer might reference "[3]" when only two documents were retrieved, or cite a document that doesn't actually support the claim. Users who trust these fake citations end up citing nonexistent sources in their own work.

Citation-verified RAG solves this in four steps: retrieve source documents, generate an answer with the LLM instructed to use inline citations, extract all citation references from the generated text, and verify each citation maps to an actual retrieved document. Invalid citations are flagged, and the verification status is included in the output.

## The Solution

**You write the citation generation and verification logic. Conductor handles the pipeline sequencing, retries, and observability.**

Each stage is an independent worker. Document retrieval, cited answer generation, citation extraction (parsing "[1]", "[2]" references from the text), and citation verification (checking each reference against the retrieved document list). Conductor sequences them and tracks every execution with the question, retrieved documents, generated answer, extracted citations, and verification results.

### What You Write: Workers

Four workers form the citation lifecycle. Document retrieval, cited answer generation, citation extraction from the answer text, and cross-reference verification against the source documents.

| Worker | Task | What It Does |
|---|---|---|
| **RetrieveDocsWorker** | `cr_retrieve_docs` | Retrieves 4 source documents with id, title, page number, text, and relevance score (0.83-0.95) from the knowledge base |
| **GenerateCitedWorker** | `cr_generate_cited` | Generates an answer with inline citation markers `[1]` `[2]` `[3]` `[4]` and produces a structured citations array mapping each marker to its source document id, page, confidence score, and the specific claim being cited |
| **ExtractCitationsWorker** | `cr_extract_citations` | Parses the generated answer text for citation markers, checks whether each marker actually appears in the answer, and reports the count of citations found vs, claimed |
| **VerifyCitationsWorker** | `cr_verify_citations` | Cross-references each citation's `docId` against the retrieved document set, flagging any citation that references a non-existent document; returns per-citation verification status and an `allVerified` boolean |

Workers simulate LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode, the workflow and worker interfaces stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
cr_retrieve_docs
    │
    ▼
cr_generate_cited
    │
    ▼
cr_extract_citations
    │
    ▼
cr_verify_citations
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
java -jar target/rag-citation-1.0.0.jar
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
| `CONDUCTOR_OPENAI_API_KEY` | _(not set)_ | OpenAI API key. When set, GenerateCitedWorker calls gpt-4o-mini instead of using simulated output |

## Example Output

```
=== RAG Citation Demo: Generate with Source Citations and Verification ===

Step 1: Registering task definitions...
  Registered: cr_retrieve_docs, cr_generate_cited, cr_extract_citations, cr_verify_citations

Step 2: Registering workflow 'citation_rag_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  [retrieve_docs] Retrieving documents for question
  [retrieve_docs] Found 4 documents
  [generate_cited] Generating cited answer for: How does Conductor work?
  [generate_cited] Generated answer with 4 citations
  [extract_citations] Extracting citations from answer
  [extract_citations] Found 4 citations in answer text
  [verify_citations] Verifying 4 citations against 4 documents
  [verify_citations] Verified 4/4 citations

  Workflow ID: b8c9d0e1-...

Step 5: Waiting for completion...
  Status: COMPLETED
  Answer: Conductor employs a task-based workflow model [1] that supports workers in
    multiple languages including Java, Python, Go, and TypeScript [2]. It also supports
    running multiple workflow versions simultaneously for safe rollouts [3]. The system
    provides automatic load balancing through task queues [4].
  Citations: 4 verified, all passed

Result: PASSED
```

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/rag-citation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow citation_rag_workflow \
  --version 1 \
  --input '{"question": "How does Conductor handle workflow orchestration?"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w citation_rag_workflow -s COMPLETED -c 5
```

## The Citation-Verified RAG Pipeline

Standard RAG generates answers from retrieved context, but there is no guarantee the LLM actually cites its sources correctly. This pipeline adds citation tracking and verification in four stages:

1. **Retrieve** (`cr_retrieve_docs`): Fetch source documents from the knowledge base, each with an id, title, page number, and relevance score.

2. **Generate with Citations** (`cr_generate_cited`): The LLM generates an answer using the retrieved documents as context, with explicit instructions to use inline citation markers (`[1]`, `[2]`, etc.). The output includes both the answer text and a structured citations array mapping each marker to its source document, page, confidence score, and the specific claim being cited.

3. **Extract Citations** (`cr_extract_citations`): Parse the generated answer text for citation markers and verify each one actually appears in the answer. This catches cases where the citations array claims a reference that does not appear in the answer text.

4. **Verify Citations** (`cr_verify_citations`): Cross-reference each citation's `docId` against the retrieved document set. This catches hallucinated citations, where the LLM references a document that was never retrieved (e.g., citing `[5]` when only 4 documents were provided).

## How to Extend

Each worker handles one citation lifecycle step. Swap in a real vector store for retrieval, instruct Claude or GPT-4 to produce inline citations, and the extraction and verification pipeline runs unchanged.

- **RetrieveDocsWorker** (`cr_retrieve_docs`): swap in a real vector store query against Pinecone, Weaviate, Qdrant, pgvector, or Elasticsearch. Include document metadata (title, page, URL) so citations can link to the source
- **GenerateCitedWorker** (`cr_generate_cited`): call Claude or GPT-4 with a system prompt that instructs inline citations: "Cite sources using [1], [2], etc. Every factual claim must have a citation." Parse the structured citations from the LLM response
- **ExtractCitationsWorker** (`cr_extract_citations`): the parsing logic is already deterministic and production-ready. Enhance it with regex-based extraction of citation markers from free-text LLM output
- **VerifyCitationsWorker** (`cr_verify_citations`): the verification logic is already deterministic and production-ready. Add content verification: check that the cited claim is actually supported by the referenced document's text (semantic similarity check)
- **Add a citation rejection loop**: if `allVerified` is false, re-generate the answer with stricter citation instructions (Conductor DO_WHILE task)

Each worker keeps its output contract stable, so upgrading the retrieval backend or tightening the citation verification logic requires no workflow changes.

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
rag-citation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ragcitation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RagCitationExample.java      # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExtractCitationsWorker.java  # Parse and validate citation markers in answer text
│       ├── GenerateCitedWorker.java     # LLM generation with inline [1][2][3][4] citations
│       ├── RetrieveDocsWorker.java      # Fetch source documents with metadata
│       └── VerifyCitationsWorker.java   # Cross-reference citations against document set
└── src/test/java/ragcitation/workers/
    ├── ExtractCitationsWorkerTest.java  # 5 tests. Marker parsing, missing markers
    ├── GenerateCitedWorkerTest.java     # Tests. Citation structure, answer format
    ├── RetrieveDocsWorkerTest.java      # Tests. Document count, metadata fields
    └── VerifyCitationsWorkerTest.java   # 7 tests. Verified/unverified, null handling
```
