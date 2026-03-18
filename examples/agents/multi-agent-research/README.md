# Multi-Agent Research in Java Using Conductor: Define, Parallel Search Across Web/Papers/Databases, Synthesize, Report

Your research intern searches Google, finds three blog posts, and writes the report. No academic papers. No internal data. The conclusions sound confident but rest on a single source type. Another intern starts from Semantic Scholar, finds contradicting peer-reviewed evidence, but never cross-references the web findings. When research agents work sequentially on one source at a time, a five-minute task takes an hour, and you still get a biased report. This example fans out three specialized search agents (web, academic papers, internal databases) in parallel using Conductor's `FORK_JOIN`, then synthesizes and cross-references their findings into a single report. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers. You write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Comprehensive Research Requires Multiple Sources

Researching a topic from a single source produces biased, incomplete findings. Web search gives you current news and opinions. Academic papers provide peer-reviewed evidence. Databases contain structured data and statistics. A comprehensive research report draws from all three, cross-referencing web findings with academic evidence and database statistics.

The three search types are independent. Web search doesn't depend on paper search, so they should run simultaneously. After all three complete, the synthesizer must merge findings, resolve contradictions between sources, and identify where multiple sources agree (high confidence) or disagree (needs further investigation). The report writer then structures everything into a coherent document with citations.

## The Solution

**You write the search, synthesis, and report generation logic. Conductor handles parallel source querying, cross-reference merging, and citation tracking.**

`DefineResearchWorker` establishes the research scope, key questions, and evaluation criteria. `FORK_JOIN` dispatches three search agents in parallel: `SearchWebWorker` finds current articles, blog posts, and news. `SearchPapersWorker` queries academic databases for peer-reviewed research. `SearchDatabasesWorker` retrieves relevant structured data and statistics. After `JOIN` collects all results, `SynthesizeWorker` merges findings across sources, resolves contradictions, and identifies consensus. `WriteReportWorker` structures the synthesis into a formatted research report with citations and confidence levels. Conductor runs all three searches simultaneously and tracks which sources contributed to each finding.

### What You Write: Workers

Six workers conduct the research. Defining scope, searching web, papers, and databases in parallel, synthesizing findings, and writing the report.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **DefineResearchWorker** | `ra_define_research` | Defines the research scope. Takes a topic and depth, produces search queries, target domains, and database names to .. | Simulated |
| **SearchDatabasesWorker** | `ra_search_databases` | Searches internal databases for proprietary findings. Takes queries and database names, returns findings with source.. | Simulated |
| **SearchPapersWorker** | `ra_search_papers` | Searches academic papers for scholarly findings. Takes queries and academic domains, returns findings with citations.. | Simulated |
| **SearchWebWorker** | `ra_search_web` | Searches the web for relevant findings. Takes queries and maxResults, returns a list of findings with source, title,.. | Simulated |
| **SynthesizeWorker** | `ra_synthesize` | Synthesizes findings from all three search agents. Computes total sources, average credibility, produces a synthesis.. | Simulated |
| **WriteReportWorker** | `ra_write_report` | Writes the final research report. Takes topic, synthesis, key insights, and source count, and produces a title, exec.. | Simulated |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode, the agent workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |
| **Parallel execution** | FORK_JOIN runs multiple tasks simultaneously and waits for all to complete |

### The Workflow

```
ra_define_research
    │
    ▼
FORK_JOIN
    ├── ra_search_web
    ├── ra_search_papers
    └── ra_search_databases
    │
    ▼
JOIN (wait for all branches)
ra_synthesize
    │
    ▼
ra_write_report
```

## Example Output

```
=== Multi-Agent Research Demo ===

Step 1: Registering task definitions...
  Registered: ra_define_research, ra_search_web, ra_search_papers, ra_search_databases, ra_synthesize, ra_write_report

Step 2: Registering workflow 'multi_agent_research'...
  Workflow registered.

Step 3: Starting workers...
  6 workers polling.

Step 4: Starting workflow...
  Workflow ID: 5055c795-816e-3f0a-5ddd-046c9afea7ee

  [ra_define_research] Defining research scope for: Impact of large language models on software engineering
  [ra_search_web] Searching web with 3 queries...
  [ra_search_papers] Searching academic papers across 3 domains...
  [ra_search_databases] Searching 3 databases...
  [ra_synthesize] Synthesizing findings for: Impact of large language models on software engineering
  [ra_write_report] Writing report on: Impact of large language models on software engineering


  Status: COMPLETED
  Output: {title=Research Report: , executiveSummary=<String.format(
     >, sections=5, wordCount=3200}
     >, sections=5, wordCount=3200}
     >, sections=5, wordCount=3200}

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
java -jar target/multi-agent-research-1.0.0.jar
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
java -jar target/multi-agent-research-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow multi_agent_research \
  --version 1 \
  --input '{"topic": "Impact of large language models on software engineering", "depth": "comprehensive"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w multi_agent_research -s COMPLETED -c 5
```

## How to Extend

Each search agent targets one source type. Integrate Tavily for web results, Semantic Scholar for academic papers, and JDBC for internal databases, and the define-search-synthesize-report workflow runs unchanged.

- **SearchWebWorker** (`ra_search_web`): integrate with Tavily, SerpAPI, or Bing Search API for real web results with snippet extraction and source credibility scoring
- **SearchPapersWorker** (`ra_search_papers`): query Semantic Scholar, arXiv, PubMed, or Google Scholar APIs for peer-reviewed papers with citation counts and relevance ranking
- **SynthesizeWorker** (`ra_synthesize`): use an LLM to perform cross-source analysis: identify agreements, contradictions, and gaps across web, academic, and database sources

Plug in real search APIs and academic databases; the research pipeline preserves the same search-synthesize-report interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):


## Project Structure

```
multi-agent-research/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/multiagentresearch/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MultiAgentResearchExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DefineResearchWorker.java
│       ├── SearchDatabasesWorker.java
│       ├── SearchPapersWorker.java
│       ├── SearchWebWorker.java
│       ├── SynthesizeWorker.java
│       └── WriteReportWorker.java
└── src/test/java/multiagentresearch/workers/
    ├── DefineResearchWorkerTest.java        # 9 tests
    ├── SearchDatabasesWorkerTest.java        # 8 tests
    ├── SearchPapersWorkerTest.java        # 8 tests
    ├── SearchWebWorkerTest.java        # 8 tests
    ├── SynthesizeWorkerTest.java        # 12 tests
    └── WriteReportWorkerTest.java        # 10 tests
```
