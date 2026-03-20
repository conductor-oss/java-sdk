# Search Agent in Java Using Conductor :  Formulate Queries, Parallel Google/Wiki Search, Rank, Synthesize

Search Agent .  formulate queries, search Google and Wikipedia in parallel, rank/merge results, and synthesize a final answer. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Good Answers Need Multiple Search Sources

A question like "What are the environmental impacts of lithium mining?" benefits from both web search (current news, industry reports, environmental assessments) and Wikipedia (background knowledge, historical context, established science). Searching either alone gives incomplete results. Searching both sequentially doubles the latency.

Parallel search across Google and Wikipedia halves the wait time. But the results need merging: Google results have snippets and URLs, Wikipedia results have structured summaries and references. Rank-merging combines them by relevance, deduplicates overlapping information, and produces a single ranked result set. The synthesis step then generates a grounded answer with citations to both web and Wikipedia sources.

## The Solution

**You write the query formulation, search execution, ranking, and synthesis logic. Conductor handles parallel search dispatch, result merging, and source attribution.**

`FormulateQueriesWorker` analyzes the question and generates targeted search queries optimized for each source .  keyword queries for Google, topic queries for Wikipedia. `FORK_JOIN` dispatches `SearchGoogleWorker` and `SearchWikiWorker` simultaneously. After `JOIN` collects both result sets, `RankMergeWorker` combines and deduplicates results by relevance score, normalizing across different source formats. `SynthesizeWorker` generates a comprehensive answer from the ranked results with inline citations to both web and Wikipedia sources. Conductor runs both searches in parallel and records the contribution of each source to the final answer.

### What You Write: Workers

Five workers run the search pipeline. Formulating queries, searching Google and Wikipedia in parallel, ranking and merging results, and synthesizing a grounded answer.

| Worker | Task | What It Does |
|---|---|---|
| **FormulateQueriesWorker** | `sa_formulate_queries` | Takes a user question and formulates optimized search queries. Returns a list of queries, the detected intent, and co... |
| **RankMergeWorker** | `sa_rank_merge` | Merges Google and Wikipedia results, sorts by relevance descending, and returns the ranked results along with top sou... |
| **SearchGoogleWorker** | `sa_search_google` | Simulates a Google search using the provided queries. Returns search results with title, url, snippet, relevance, and... |
| **SearchWikiWorker** | `sa_search_wiki` | Simulates a Wikipedia search using the provided queries. Returns search results with title, url, snippet, relevance, ... |
| **SynthesizeWorker** | `sa_synthesize` | Synthesizes a final answer from ranked search results and top sources. Produces a coherent answer string, confidence ... |

Workers simulate agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode .  the agent workflow stays the same.

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
sa_formulate_queries
    │
    ▼
FORK_JOIN
    ├── sa_search_google
    └── sa_search_wiki
    │
    ▼
JOIN (wait for all branches)
sa_rank_merge
    │
    ▼
sa_synthesize
```

## Example Output

```
=== Search Agent Demo ===

Step 1: Registering task definitions...
  Registered: sa_formulate_queries, sa_search_google, sa_search_wiki, sa_rank_merge, sa_synthesize

Step 2: Registering workflow 'search_agent'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [sa_formulate_queries] Formulating queries for:
  [sa_rank_merge] Merging
  [sa_search_google] Searching Google with
  [sa_search_wiki] Searching Wikipedia with
  [sa_synthesize] Synthesizing answer from

  Status: COMPLETED
  Output: {queries=..., intent=..., complexity=..., originalQuestion=...}

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
java -jar target/search-agent-1.0.0.jar
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
java -jar target/search-agent-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow search_agent \
  --version 1 \
  --input '{"question": "sample-question", "What is the current state of quantum computing in 2026?": "sample-What is the current state of quantum computing in 2026?", "maxResults": "sample-maxResults"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w search_agent -s COMPLETED -c 5
```

## How to Extend

Each search worker targets one source. Integrate SerpAPI or Google Custom Search for web results, the Wikipedia API for encyclopedic knowledge, and an LLM for cited answer synthesis, and the formulate-search-rank-synthesize workflow runs unchanged.

- **SearchGoogleWorker** (`sa_search_google`): integrate with SerpAPI, Google Custom Search JSON API, or Tavily for real web search with snippet extraction and source credibility scoring
- **SearchWikiWorker** (`sa_search_wiki`): use the Wikipedia API's `action=query&list=search` endpoint for full-text search, with `prop=extracts` for page content retrieval
- **SynthesizeWorker** (`sa_synthesize`): use an LLM with the ranked results as context, instructing it to cite sources using numbered references and note where sources agree or disagree

Connect to real search engines and Wikipedia API; the parallel search pipeline preserves the same query-rank-synthesize interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):


## Project Structure

```
search-agent/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/searchagent/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SearchAgentExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── FormulateQueriesWorker.java
│       ├── RankMergeWorker.java
│       ├── SearchGoogleWorker.java
│       ├── SearchWikiWorker.java
│       └── SynthesizeWorker.java
└── src/test/java/searchagent/workers/
    ├── FormulateQueriesWorkerTest.java        # 9 tests
    ├── RankMergeWorkerTest.java        # 8 tests
    ├── SearchGoogleWorkerTest.java        # 8 tests
    ├── SearchWikiWorkerTest.java        # 8 tests
    └── SynthesizeWorkerTest.java        # 9 tests
```
