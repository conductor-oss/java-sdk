# Summarization Pipeline in Java with Conductor :  Extract Sections, Compress, and Generate Summaries

A Java Conductor workflow that summarizes long documents .  extracting logical sections from the input, compressing each section to its key points, and generating a cohesive summary within a specified maximum length. Given a `document` and `maxLength`, the pipeline produces extracted sections, compressed content, and a final summary. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the three-step summarization pipeline.

## Turning Long Documents into Actionable Summaries

Long documents .  reports, articles, meeting transcripts, legal filings ,  contain important information buried in pages of text. Reading every word is impractical when you need the key points. Effective summarization requires structure: first identify the document's logical sections (introduction, findings, recommendations), then compress each section to its essential points, and finally generate a cohesive summary that stays within a target length.

This workflow processes a document through three summarization steps. The section extractor identifies logical segments in the text. The compressor reduces each section to its key points while preserving meaning. The summary generator combines the compressed sections into a final summary that respects the `maxLength` constraint. Each step builds on the previous one .  you cannot compress sections that have not been identified, and you cannot generate a summary without compressed content.

## The Solution

**You just write the section-extraction, compression, and summary-generation workers. Conductor handles the summarization pipeline and content flow.**

Three workers form the summarization pipeline .  section extraction, compression, and summary generation. The extractor identifies logical document sections. The compressor reduces each section to its essential points. The generator produces a final summary within the specified length. Conductor sequences the three steps and passes sections and compressed content between them via JSONPath.

### What You Write: Workers

ExtractSectionsWorker identifies logical document segments, CompressWorker reduces each section to key points, and GenerateSummaryWorker combines the compressed content into a cohesive summary within the target length.

| Worker | Task | What It Does |
|---|---|---|
| **CompressWorker** | `sum_compress` | Reduces each extracted section to its key points, achieving ~80% word-count reduction while preserving meaning. |
| **ExtractSectionsWorker** | `sum_extract_sections` | Identifies and extracts logical sections (intro, findings, recommendations, etc.) from the input document. |
| **GenerateSummaryWorker** | `sum_generate_summary` | Combines compressed sections into a final cohesive summary within the specified maximum length. |

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
sum_extract_sections
    │
    ▼
sum_compress
    │
    ▼
sum_generate_summary
```

## Example Output

```
=== Example 636: Summarization Pipeline ===

Step 1: Registering task definitions...
  Registered: sum_extract_sections, sum_compress, sum_generate_summary

Step 2: Registering workflow 'sum_summarization_pipeline'...
  Workflow registered.

Step 3: Starting workers...
  3 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [compress] Sections compressed: 2150 -> 420 words (80% reduction)
  [extract] Extracted
  [generate] Summary generated (

  Status: COMPLETED
  Output: {compressed=..., compressionRatio=..., compressedWords=..., sections=...}

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
java -jar target/summarization-pipeline-1.0.0.jar
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
java -jar target/summarization-pipeline-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow sum_summarization_pipeline \
  --version 1 \
  --input '{"document": "sample-document", "Full research paper text...": "Sample Full research paper text...", "maxLength": "sample-maxLength"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w sum_summarization_pipeline -s COMPLETED -c 5
```

## How to Extend

Each worker handles one summarization stage .  connect your LLM (Claude, GPT-4) for section compression and final generation, and the summarization workflow stays the same.

- **CompressWorker** (`sum_compress`): use an LLM (GPT-4, Claude) for intelligent content compression that preserves key details
- **ExtractSectionsWorker** (`sum_extract_sections`): integrate with document parsers (Apache Tika, PDF.js) for real section extraction from PDFs and Word docs
- **GenerateSummaryWorker** (`sum_generate_summary`): swap in an LLM with explicit length constraints for production-grade summary generation

Plug in a production LLM for compression and the extract-compress-summarize pipeline operates without modification.

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
summarization-pipeline/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/summarizationpipeline/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SummarizationPipelineExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CompressWorker.java
│       ├── ExtractSectionsWorker.java
│       └── GenerateSummaryWorker.java
└── src/test/java/summarizationpipeline/workers/
    ├── CompressWorkerTest.java        # 2 tests
    ├── ExtractSectionsWorkerTest.java        # 2 tests
    └── GenerateSummaryWorkerTest.java        # 2 tests
```
