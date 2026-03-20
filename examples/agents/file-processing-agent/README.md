# File Processing Agent in Java Using Conductor :  Detect Type, Extract Content, Analyze, Summarize

File Processing Agent .  detect file type, extract content, analyze, and generate summary through a sequential pipeline. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Different File Types Need Different Processing

A user uploads a file. Is it a PDF contract that needs text extraction and clause identification? A CSV dataset that needs column analysis and statistical summaries? A JSON config file that needs schema validation? An image that needs OCR or object detection? The processing pipeline must adapt to the file type.

File type detection goes beyond MIME types .  a `.csv` file might actually be tab-separated, a `.json` file might be malformed, a PDF might be scanned (requiring OCR) or native (requiring text extraction). Each file type needs a different extraction strategy, and the analysis step needs to know what kind of content it's working with (tabular data vs, free text vs: structured config). Without orchestration, this becomes a monolithic processor with nested type-checking and extraction logic that's impossible to extend.

## The Solution

**You write the file detection, extraction, analysis, and summarization logic. Conductor handles the processing pipeline, retries on extraction failures, and per-file-type performance tracking.**

`DetectFileTypeWorker` examines the file metadata (name, size, MIME type) and content signatures to determine the actual file type and processing strategy. `ExtractContentWorker` applies the appropriate extraction method .  text extraction for PDFs, parsing for CSVs, deserialization for JSON. `AnalyzeContentWorker` performs type-appropriate analysis. NLP for text content, statistical summaries for tabular data, schema validation for structured data. `GenerateSummaryWorker` produces a structured summary tailored to the content type. Conductor chains these steps and tracks processing time per file type for performance optimization.

### What You Write: Workers

Four workers process uploaded files. Detecting the type, extracting content with the appropriate method, analyzing it, and generating a structured summary.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeContentWorker** | `fp_analyze_content` | Analyzes extracted content .  identifies document type, sentiment, topics, named entities, and key findings. |
| **DetectFileTypeWorker** | `fp_detect_file_type` | Detects the file type from the file name extension and returns the file type, extraction method, and category. |
| **ExtractContentWorker** | `fp_extract_content` | Simulates content extraction from a file. Returns structured content (title, sections, pages) and metadata (word coun... |
| **GenerateSummaryWorker** | `fp_generate_summary` | Generates a human-readable summary from the analysis results and key findings. |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode .  the agent workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
fp_detect_file_type
    │
    ▼
fp_extract_content
    │
    ▼
fp_analyze_content
    │
    ▼
fp_generate_summary
```

## Example Output

```
=== File Processing Agent Demo ===

Step 1: Registering task definitions...
  Registered: fp_detect_file_type, fp_extract_content, fp_analyze_content, fp_generate_summary

Step 2: Registering workflow 'file_processing_agent'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [fp_analyze_content] Analyzing content of type:
  [fp_detect_file_type] Detecting file type for:
  [fp_extract_content] Extracting content from:
  [fp_generate_summary] Generating summary for:

  Status: COMPLETED
  Output: {analysis=..., keyFindings=..., fileType=..., extractionMethod=...}

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
java -jar target/file-processing-agent-1.0.0.jar
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
java -jar target/file-processing-agent-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow file_processing_agent \
  --version 1 \
  --input '{"fileName": "sample-name", "acme_q4_2025_financial_report.pdf": "sample-acme-q4-2025-financial-report.pdf", "fileSize": 5, "mimeType": "standard"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w file_processing_agent -s COMPLETED -c 5
```

## How to Extend

Each worker handles one file processing stage. Integrate Apache Tika for universal parsing, Tesseract OCR for scanned documents, and NLP libraries for content analysis, and the detect-extract-analyze-summarize workflow runs unchanged.

- **ExtractContentWorker** (`fp_extract_content`): integrate Apache Tika for universal file parsing, Apache PDFBox for PDF text extraction, or Tesseract OCR for scanned documents and images
- **AnalyzeContentWorker** (`fp_analyze_content`): use domain-specific analyzers: spaCy/Stanford NLP for text entity extraction, pandas-like analysis for CSVs, JSONSchema validation for config files
- **DetectFileTypeWorker** (`fp_detect_file_type`): use Apache Tika's content detection for accurate MIME type identification beyond file extensions, with magic byte analysis for misnamed files

Swap in real PDF/CSV parsers and NLP analysis; the file processing pipeline maintains the same detect-extract-analyze-summarize contract.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):


## Project Structure

```
file-processing-agent/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/fileprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FileProcessingAgentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeContentWorker.java
│       ├── DetectFileTypeWorker.java
│       ├── ExtractContentWorker.java
│       └── GenerateSummaryWorker.java
└── src/test/java/fileprocessing/workers/
    ├── AnalyzeContentWorkerTest.java        # 9 tests
    ├── DetectFileTypeWorkerTest.java        # 9 tests
    ├── ExtractContentWorkerTest.java        # 9 tests
    └── GenerateSummaryWorkerTest.java        # 9 tests
```
