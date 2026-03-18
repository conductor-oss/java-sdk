# PDF Processing in Java Using Conductor: Text Extraction, Section Parsing, Content Analysis, and Summary Generation

Five hundred vendor invoices arrive in accounts payable every month as PDF attachments. They come in 12 different formats: some with line items in tables, some with totals buried in paragraph text, some scanned at odd angles. An intern spends 3 days each month opening them one by one, copying line items into a spreadsheet, and hoping they don't transpose a digit on a $47,000 invoice. Last quarter they did, and nobody caught it until the vendor called about an unpaid balance. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate a PDF processing pipeline, text extraction, section parsing, content analysis, and summary generation, as independent workers.

## The Problem

You receive contracts, reports, or research papers as multi-page PDFs, and you need to extract structured information from them. A 50-page contract needs to be broken into its sections (Introduction, Terms, Liability, Termination), each section needs keyword analysis to flag important clauses, and the whole document needs a summary that a reviewer can scan in 30 seconds. Raw PDF text extraction gives you a wall of characters with no structure. Splitting by headings requires knowing the heading patterns for this document type. Analysis depends on having clean, section-delimited text. The summary needs both the parsed sections and the analysis results to produce something useful.

Without orchestration, you'd write a single method that opens the PDF, extracts text, splits by regex, counts words, and outputs a summary in one pass. If section parsing fails because the heading pattern doesn't match this document's formatting, you'd re-extract the entire PDF from scratch. Even though extraction succeeded. There's no record of how many pages were extracted, how many sections were found, or what the raw text looked like before parsing. Adding a new analysis type (sentiment analysis, entity extraction, clause classification) means modifying tightly coupled code with no visibility into which step is the bottleneck.

## The Solution

**You just write the text extraction, section parsing, content analysis, and summary generation workers. Conductor handles sequential document processing, retries when PDF sources are unavailable, and tracking of page count, section count, and word count at every stage.**

Each stage of the PDF pipeline is a simple, independent worker. The text extractor loads the PDF from the source URL and produces raw text along with a page count. The section parser splits the raw text into logical sections by detecting heading patterns (chapter titles, numbered sections, bold headings), producing a structured list of sections with titles and content. The content analyzer scans the parsed sections to compute word counts and extract keywords that characterize each section. The summary generator combines the section structure and analysis results into a concise document summary. Conductor executes them in strict sequence, passes the evolving document representation between stages, retries if the PDF source is temporarily unavailable, and tracks page count, section count, and word count at every stage. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Four workers handle PDF processing: extracting raw text with page counts, parsing text into logical sections by heading patterns, analyzing content for word counts and keywords, and generating a structured document summary.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| `ExtractTextWorker` | `pd_extract_text` | Loads the PDF from the source URL and returns raw text, page count (3), and character count | Simulated |
| `ParseSectionsWorker` | `pd_parse_sections` | Splits raw text into logical sections by detecting "Chapter N:" heading patterns, returning title/content pairs | Simulated |
| `AnalyzeContentWorker` | `pd_analyze_content` | Counts words across all sections, extracts keywords (data, processing, architecture, pipelines, analytics), and computes average words per section | Simulated |
| `GenerateSummaryWorker` | `pd_generate_summary` | Combines section titles and analysis metrics into a one-line summary like "Document contains 3 chapters covering..." | Simulated |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks, the pipeline structure and error handling stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
pd_extract_text
    │
    ▼
pd_parse_sections
    │
    ▼
pd_analyze_content
    │
    ▼
pd_generate_summary
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
java -jar target/pdf-processing-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

### Sample Output

```
=== PDF Processing Demo ===

Step 1: Registering task definitions...
  Registered: pd_extract_text, pd_parse_sections, pd_analyze_content, pd_generate_summary

Step 2: Registering workflow 'pdf_processing'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...

  [extract] Extracted text from https://example.com/docs/data-strategy-2024.pdf. 3 pages, 480 chars
  [parse] Parsed 3 sections: Introduction, Architecture, Implementation
  [analyze] 67 words, 4 keywords detected: data, processing, architecture, pipelines
  [summary] Generated summary (112 chars)

  Workflow ID: 3fa85f64-5542-4562-b3fc-2c963f66afa6

Step 5: Waiting for completion...
  Status: COMPLETED
  Output: {pageCount=3, sectionCount=3, wordCount=67, summary=Document contains 3 chapters covering introduction, architecture, implementation. Average 22 words per section.}

Result: PASSED
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/pdf-processing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow pdf_processing \
  --version 1 \
  --input '{"pdfUrl": "https://example.com/docs/data-strategy-2024.pdf", "options": {"extractImages": false, "ocrFallback": true}}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w pdf_processing -s COMPLETED -c 5
```

## How to Extend

Use Apache PDFBox for real text extraction, add NLP-based keyword analysis with spaCy or OpenAI, and the document processing workflow runs unchanged.

- **`ExtractTextWorker`**: Use real PDF libraries: Apache PDFBox for text extraction with layout preservation, iText for structured PDF parsing, or Tika for format-agnostic document extraction including embedded images and metadata.

- **`ParseSectionsWorker`**: Implement real section detection: heading font-size analysis via PDFBox, table-of-contents parsing, regex-based section splitting for known document formats (legal contracts, academic papers, financial reports).

- **`AnalyzeContentWorker`**: Run real content analysis: TF-IDF keyword extraction, named entity recognition (NER) for people/organizations/dates, clause classification for contracts, or LLM-based analysis via OpenAI/Anthropic APIs.

- **`GenerateSummaryWorker`**: Generate real summaries using extractive summarization (TextRank, LSA) or abstractive summarization via LLMs, with configurable summary length and focus areas per document type.

Replacing the extractor with Apache PDFBox or adding sentiment analysis to the content analyzer does not alter the extract-parse-analyze-summarize pipeline, as long as each worker outputs the expected section and keyword structures.

**Add new stages** by inserting tasks in `workflow.json`, for example, a table extraction step that detects and parses tabular data within the PDF, a compliance check step that flags missing required sections in regulatory filings, or a comparison step that diffs the current document against a previous version to highlight changes.

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
pdf-processing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/pdfprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PdfProcessingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeContentWorker.java
│       ├── ExtractTextWorker.java
│       ├── GenerateSummaryWorker.java
│       └── ParseSectionsWorker.java
└── src/test/java/pdfprocessing/workers/
    ├── AnalyzeContentWorkerTest.java
    ├── ExtractTextWorkerTest.java
    ├── GenerateSummaryWorkerTest.java
    └── ParseSectionsWorkerTest.java
```
