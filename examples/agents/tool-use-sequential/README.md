# Sequential Tool Use in Java Using Conductor :  Search, Read Page, Extract Data, Summarize

Tool Use Sequential: search the web, read a page, extract data, and summarize through a sequential pipeline of demo tool calls. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Some Tools Need to Be Called in Order

"Find the latest quarterly revenue for Acme Corp and summarize the key drivers" requires four sequential steps: search the web for Acme Corp quarterly results, read the most relevant page (likely an earnings report or press release), extract the revenue figures and key metrics from the page content, and summarize the findings in natural language.

Each step depends on the previous one. you can't read a page without a URL from search results, you can't extract data without page content, and you can't summarize without extracted data. If the page read fails (timeout, paywall), you need to retry without re-running the search. And you want to see each step's output: which URL was selected, what content was extracted, what data was found.

## The Solution

**You write the search, page reading, data extraction, and summarization logic. Conductor handles the sequential tool chain, per-step retries, and full research trail recording.**

`SearchWebWorker` queries a search engine and returns ranked results with URLs, titles, and snippets. `ReadPageWorker` fetches the top result URL and extracts the page content (text, tables, metadata). `ExtractDataWorker` parses the page content to extract structured data. numbers, dates, entities, relationships. `SummarizeWorker` generates a natural language summary from the extracted data. Conductor chains these four tools in sequence, passing each output to the next, and records the full chain for debugging.

### What You Write: Workers

Four workers chain tool calls in order. Searching the web, reading the top page, extracting structured data, and summarizing the findings.

| Worker | Task | What It Does |
|---|---|---|
| **ExtractDataWorker** | `ts_extract_data` | Simulates extracting structured data from page content. Takes pageContent and query, returns extracted facts, key fea... |
| **ReadPageWorker** | `ts_read_page` | Simulates reading a web page. Takes a url and title, returns the page content including sections and word count. |
| **SearchWebWorker** | `ts_search_web` | Simulates a web search tool. Takes a query and maxResults, returns a list of search results with url, title, and snip... |
| **SummarizeWorker** | `ts_summarize` | Simulates summarizing extracted data into a coherent summary. Takes query, extractedData, sourceUrl, and sourceTitle.... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
ts_search_web
    │
    ▼
ts_read_page
    │
    ▼
ts_extract_data
    │
    ▼
ts_summarize

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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/tool-use-sequential-1.0.0.jar

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
java -jar target/tool-use-sequential-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tool_use_sequential \
  --version 1 \
  --input '{"query": "What is workflow orchestration?", "maxResults": "sample-maxResults"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tool_use_sequential -s COMPLETED -c 5

```

## How to Extend

Each worker wraps one tool in the research chain. Integrate SerpAPI for web search, Jsoup or Playwright for page reading, and an LLM for structured data extraction, and the search-read-extract-summarize pipeline runs unchanged.

- **SearchWebWorker** (`ts_search_web`): integrate with SerpAPI, Bing Search API, or Tavily for real search results with snippet extraction and result ranking
- **ReadPageWorker** (`ts_read_page`): use Jsoup or Playwright for web page fetching with JavaScript rendering, paywall detection, and structured content extraction
- **ExtractDataWorker** (`ts_extract_data`): use an LLM with the page content to extract structured data (revenue figures, dates, names) into a JSON schema, or use spaCy for NER-based extraction

Replace with real web scraping and NLP extraction; the search-read-extract-summarize pipeline keeps the same data flow interface.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
tool-use-sequential/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/toolusesequential/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ToolUseSequentialExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExtractDataWorker.java
│       ├── ReadPageWorker.java
│       ├── SearchWebWorker.java
│       └── SummarizeWorker.java
└── src/test/java/toolusesequential/workers/
    ├── ExtractDataWorkerTest.java        # 9 tests
    ├── ReadPageWorkerTest.java        # 9 tests
    ├── SearchWebWorkerTest.java        # 8 tests
    └── SummarizeWorkerTest.java        # 9 tests

```
