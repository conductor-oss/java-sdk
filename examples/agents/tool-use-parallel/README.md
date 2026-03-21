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

| Worker | Task | What It Does |
|---|---|---|
| **PlanToolsWorker** | `tp_plan_tools` | Analyzes the user request and determines which tools to invoke in parallel. Returns tool configurations: weather units (fahrenheit) and hourly toggle, news topics (technology, business, world), and stock tickers (AAPL, GOOGL, MSFT, AMZN). Reports tool count of 3. |
| **CallWeatherWorker** | `tp_call_weather` | Fetches weather data for the given location. Returns current conditions (58F, Morning Fog, 85% humidity), 4-entry hourly forecast (08:00-14:00), and daily high/low (68F/55F). |
| **CallNewsWorker** | `tp_call_news` | Retrieves top headlines across requested topic categories. Returns 4 headlines with titles, topics (technology, business, world), and sources (TechDaily, MarketWatch, WorldNews, Bloomberg). |
| **CallStocksWorker** | `tp_call_stocks` | Fetches stock market quotes for requested tickers. Returns 4 quotes with price, change, and change percent (AAPL +1.33%, GOOGL +1.11%, MSFT -0.20%, AMZN +1.74%), plus overall market sentiment (bullish). |
| **CombineResultsWorker** | `tp_combine_results` | Merges parallel weather, news, and stocks data into a unified morning briefing object. Produces a human-readable summary paragraph and records the list of tools used and generation timestamp. |

The demo workers produce realistic, deterministic output shapes so the workflow runs end-to-end. To go to production, replace the simulation with the real API call, the worker interface stays the same, and no workflow changes are needed.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
