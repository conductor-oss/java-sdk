# Documentation AI in Java with Conductor :  Auto-Generate Docs from Source Code

A Java Conductor workflow that generates documentation from source code .  analyzing a repository to discover modules and their structure, generating raw documentation for each module, formatting the output in the requested format (Markdown, HTML, etc.), and publishing the docs to a hosting platform. Given a `repoPath` and `outputFormat`, the pipeline produces module counts, page counts, formatted documentation, and a publish URL. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the analyze-generate-format-publish pipeline.

## Keeping Documentation in Sync with Code

Documentation drifts out of sync with code because writing it is manual and updating it is an afterthought. Developers change function signatures, add modules, and refactor classes; but the docs stay frozen at the last time someone remembered to update them. Automating documentation generation from the source code itself eliminates this drift.

This workflow reads a codebase and produces publishable documentation. The code analyzer scans the repository to discover modules, classes, functions, and their relationships. The doc generator produces raw documentation pages for each module. The formatter converts the raw docs into the requested output format (Markdown, HTML, or PDF). The publisher uploads the formatted docs to a hosting platform and returns the URL. Each step builds on the previous one .  you cannot format docs that have not been generated, and you cannot generate docs without first understanding the code structure.

## The Solution

**You just write the code-analysis, doc-generation, formatting, and publishing workers. Conductor handles the pipeline sequencing and output routing.**

Four workers handle the documentation lifecycle .  code analysis, doc generation, formatting, and publishing. The analyzer discovers modules and their structure. The generator creates documentation pages for each module. The formatter renders the pages in the requested output format. The publisher deploys the formatted docs and returns a URL. Conductor sequences the pipeline and passes module lists, raw docs, and formatted docs between steps via JSONPath.

### What You Write: Workers

AnalyzeCodeWorker discovers modules and function signatures, GenerateDocsWorker produces documentation pages, FormatWorker renders them in Markdown or HTML, and PublishWorker deploys to the hosting platform.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeCodeWorker** | `doc_analyze_code` | Scans the repository to discover modules, classes, and function signatures. |
| **FormatWorker** | `doc_format` | Converts raw documentation into the requested output format (Markdown, HTML, or PDF). |
| **GenerateDocsWorker** | `doc_generate_docs` | Generates documentation pages for each discovered module with descriptions and usage examples. |
| **PublishWorker** | `doc_publish` | Uploads formatted documentation to a hosting platform and returns the published URL. |

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
doc_analyze_code
    │
    ▼
doc_generate_docs
    │
    ▼
doc_format
    │
    ▼
doc_publish
```

## Example Output

```
=== Example 645: Documentation AI ===

Step 1: Registering task definitions...
  Registered: doc_analyze_code, doc_generate_docs, doc_format, doc_publish

Step 2: Registering workflow 'doc_documentation_ai'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [analyze] Found
  [format] Formatted docs as
  [generate] Generated
  [publish] Published documentation

  Status: COMPLETED
  Output: {modules=..., moduleCount=..., formattedDocs=..., format=...}

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
java -jar target/documentation-ai-1.0.0.jar
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
java -jar target/documentation-ai-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow doc_documentation_ai \
  --version 1 \
  --input '{"repoPath": "/api/v1/resource", "acme/platform": "sample-acme/platform", "outputFormat": "sample-outputFormat"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w doc_documentation_ai -s COMPLETED -c 5
```

## How to Extend

Each worker handles one documentation step .  connect your code parser (tree-sitter, JavaParser) for analysis and your docs platform (ReadTheDocs, GitBook, Docusaurus) for publishing, and the auto-docs workflow stays the same.

- **AnalyzeCodeWorker** (`doc_analyze_code`): integrate with tree-sitter or JavaParser for real AST-based code analysis
- **FormatWorker** (`doc_format`): use Docusaurus, MkDocs, or Sphinx to produce real formatted documentation sites
- **GenerateDocsWorker** (`doc_generate_docs`): swap in an LLM (GPT-4, Claude) to generate human-readable explanations from parsed code structure

Connect your real codebase scanner and doc hosting platform and the analyze-generate-format-publish pipeline operates unchanged.

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
documentation-ai/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/documentationai/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DocumentationAiExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeCodeWorker.java
│       ├── FormatWorker.java
│       ├── GenerateDocsWorker.java
│       └── PublishWorker.java
└── src/test/java/documentationai/workers/
    ├── AnalyzeCodeWorkerTest.java        # 2 tests
    ├── FormatWorkerTest.java        # 2 tests
    ├── GenerateDocsWorkerTest.java        # 2 tests
    └── PublishWorkerTest.java        # 2 tests
```
