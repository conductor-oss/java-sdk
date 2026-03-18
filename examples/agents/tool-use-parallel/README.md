# Parallel Tool Use in Java Using Conductor: Plan, Call Weather/News/Stocks Simultaneously, Combine

Your agent calls the weather API, waits 3 seconds for the response, then calls the news API, waits 2 seconds, then calls the stock market API, waits another 2 seconds. Total latency: 7 seconds for data that could have been fetched in 3 seconds if the calls ran in parallel. Sequential tool use is the default in most agent frameworks, and it makes multi-source queries painfully slow. This example uses [Conductor](https://github.com/conductor-oss/conductor) to dispatch weather, news, and stock API calls simultaneously via `FORK_JOIN`, combine the results into a single morning briefing, and deliver it in the time of the slowest call. . Not the sum of all three.

## Multi-Tool Queries Shouldn't Wait in Line

A question like "Give me a morning briefing for San Francisco" needs weather (58F, morning fog clearing to sunny), news (top technology, business, and world headlines), and stock market data (AAPL, GOOGL, MSFT, AMZN quotes). Called sequentially, each API takes 1-2 seconds. Totaling 3-6 seconds. Called in parallel, the total is 1-2 seconds (the slowest API).

Parallel tool use requires determining which tools are needed (the planning step), dispatching all tool calls simultaneously, handling partial failures (stocks API is down but weather and news responded), and combining heterogeneous results (weather data, news headlines, market numbers) into a coherent response.

## The Solution

**You write the tool planning, API integrations, and result combination logic. Conductor handles parallel dispatch, partial failure handling, and per-tool response time tracking.**

`PlanToolsWorker` analyzes the user's request and determines which tools to call and with what parameters. `FORK_JOIN` dispatches three tool calls simultaneously: `CallWeatherWorker` fetches weather data with current conditions and hourly forecast, `CallNewsWorker` retrieves headlines across technology, business, and world topics, and `CallStocksWorker` gets market quotes with price changes and sentiment. After `JOIN` collects all three results, `CombineResultsWorker` merges the heterogeneous outputs into a unified morning briefing with a human-readable summary. Conductor runs all three API calls in parallel and records each tool's response time for performance monitoring.

### What You Write: Workers

Five workers deliver the morning briefing. Planning which tools to call, then dispatching weather, news, and stock APIs in parallel before combining results.

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **PlanToolsWorker** | `tp_plan_tools` | Analyzes the user request and determines which tools to invoke in parallel. Returns tool configurations: weather units (fahrenheit) and hourly toggle, news topics (technology, business, world), and stock tickers (AAPL, GOOGL, MSFT, AMZN). Reports tool count of 3. | Simulated. Swap in an LLM to dynamically select tools |
| **CallWeatherWorker** | `tp_call_weather` | Fetches weather data for the given location. Returns current conditions (58F, Morning Fog, 85% humidity), 4-entry hourly forecast (08:00-14:00), and daily high/low (68F/55F). | Simulated. Swap in OpenWeatherMap or WeatherAPI |
| **CallNewsWorker** | `tp_call_news` | Retrieves top headlines across requested topic categories. Returns 4 headlines with titles, topics (technology, business, world), and sources (TechDaily, MarketWatch, WorldNews, Bloomberg). | Simulated. Swap in NewsAPI or Google News API |
| **CallStocksWorker** | `tp_call_stocks` | Fetches stock market quotes for requested tickers. Returns 4 quotes with price, change, and change percent (AAPL +1.33%, GOOGL +1.11%, MSFT -0.20%, AMZN +1.74%), plus overall market sentiment (bullish). | Simulated. Swap in Alpha Vantage or Polygon.io |
| **CombineResultsWorker** | `tp_combine_results` | Merges parallel weather, news, and stocks data into a unified morning briefing object. Produces a human-readable summary paragraph and records the list of tools used and generation timestamp. | Simulated. Swap in an LLM for natural language synthesis |

The simulated workers produce realistic, deterministic output shapes so the workflow runs end-to-end. To go to production, replace the simulation with the real API call, the worker interface stays the same, and no workflow changes are needed.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Parallel execution** | `FORK_JOIN` runs weather, news, and stocks API calls simultaneously and waits for all to complete |
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
tp_plan_tools
    |
    v
FORK_JOIN
    +-- tp_call_weather
    +-- tp_call_news
    +-- tp_call_stocks
    |
    v
JOIN (wait for all branches)
    |
    v
tp_combine_results
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
java -jar target/tool-use-parallel-1.0.0.jar
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

## Example Output

```
=== Tool Use Parallel Demo ===

Step 1: Registering task definitions...
  Registered: tp_plan_tools, tp_call_weather, tp_call_news, tp_call_stocks, tp_combine_results

Step 2: Registering workflow 'tool_use_parallel'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  [tp_plan_tools] Planning tools for request: Give me my morning briefing (location: San Francisco, CA)
  [tp_call_weather] Fetching weather for: San Francisco, CA
  [tp_call_news] Fetching news for topics: {weather=weatherConfig, newsTopics=newsTopics, tickers=tickers}
  [tp_call_stocks] Fetching stock quotes for: {weather=weatherConfig, newsTopics=newsTopics, tickers=tickers}
  [tp_combine_results] Combining weather, news, and stocks into briefing

  Workflow ID: 9c8d7e6f-5a4b-3c2d-1e0f-a9b8c7d6e5f4

Step 5: Waiting for completion...

  Status: COMPLETED
  Output: {briefing={weather=weather != null ? weather : Map.of(), news=news != null ? news : List.of(), stocks=stocks != null ? stocks : List.of(), summary=Good morning! Current conditions show Morning Fog at 58F with a high of 68F expected. "
                        + "In the news: AI breakthroughs and strong tech earnings dominate headlines. "
                        + "Markets are bullish with AAPL, GOOGL, and AMZN all up over 1%.}, toolsUsed=[weather_api, news_api, stock_api], generatedAt=2026-03-08T08:00:00Z}

Result: PASSED
```
## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/tool-use-parallel-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
# Morning briefing for San Francisco
conductor workflow start \
  --workflow tool_use_parallel \
  --version 1 \
  --input '{"userRequest": "Give me my morning briefing", "location": "San Francisco, CA"}'

# Briefing for New York
conductor workflow start \
  --workflow tool_use_parallel \
  --version 1 \
  --input '{"userRequest": "What do I need to know today?", "location": "New York, NY"}'

# Market-focused briefing for London
conductor workflow start \
  --workflow tool_use_parallel \
  --version 1 \
  --input '{"userRequest": "Give me a quick market update with weather", "location": "London, UK"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tool_use_parallel -s COMPLETED -c 5
```

## How to Extend

Each tool worker wraps one data API. Integrate OpenWeatherMap for weather, NewsAPI for headlines, and Alpha Vantage for stock quotes, and the plan-fork-combine parallel tool workflow runs unchanged.

- **PlanToolsWorker** (`tp_plan_tools`): use OpenAI function calling or Anthropic tool use to dynamically select which tools to invoke based on the user's request, including conditional tool selection (skip stocks on weekends, add calendar on workdays)
- **CallWeatherWorker** (`tp_call_weather`): integrate with OpenWeatherMap (free tier: 1000 calls/day), WeatherAPI, or NOAA APIs for real weather data with 7-day forecasts and severe weather alerts
- **CallNewsWorker** (`tp_call_news`): use NewsAPI (free tier: 100 requests/day), Google News RSS feeds, or Bing News Search for real headlines with publication time, source credibility, and article links
- **CallStocksWorker** (`tp_call_stocks`): connect to Alpha Vantage (free tier: 25 requests/day), Yahoo Finance via yfinance, or Polygon.io for real-time market data with historical charts and technical indicators
- **Add a new tool**: create a new worker class, add a branch to the `FORK_JOIN` in `workflow.json`, update the `JOIN` on clause, and extend `CombineResultsWorker` to include the new data source. No existing code changes needed.

Plug in OpenWeatherMap, NewsAPI, and Alpha Vantage; the parallel tool pipeline preserves the same plan-dispatch-combine interface.

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
tool-use-parallel/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/tooluseparallel/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ToolUseParallelExample.java  # Main entry point (supports --workers mode)
│   └── workers/
│       ├── PlanToolsWorker.java     # Selects tools and configures parameters
│       ├── CallWeatherWorker.java   # Weather with current, hourly, and high/low
│       ├── CallNewsWorker.java      # Headlines across technology, business, world
│       ├── CallStocksWorker.java    # Stock quotes with price changes and sentiment
│       └── CombineResultsWorker.java # Merges all tool outputs into unified briefing
└── src/test/java/tooluseparallel/workers/
    ├── PlanToolsWorkerTest.java     # 8 tests
    ├── CallWeatherWorkerTest.java   # 8 tests
    ├── CallNewsWorkerTest.java      # 8 tests
    ├── CallStocksWorkerTest.java    # 9 tests
    └── CombineResultsWorkerTest.java # 9 tests
```
