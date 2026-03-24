# Tool Use Parallel: Morning Briefing via FORK_JOIN

A user wants a morning briefing covering weather, news, and stock prices. Calling each API sequentially would triple the latency. Since the three data sources are independent, they should be fetched in parallel and merged into a single briefing after all three return.

This workflow plans which tools to call, dispatches weather, news, and stock API calls in parallel via a FORK_JOIN task, waits at a JOIN, and combines the results into a unified morning briefing.

## Pipeline Architecture

```
userRequest, location
       |
       v
tp_plan_tools            (toolConfigs for weather, news, stocks)
       |
       v
  FORK_JOIN
    |          |            |
    v          v            v
tp_call     tp_call      tp_call
_weather    _news        _stocks
    |          |            |
    +-----+----+----+------+
          |
          v
       JOIN
          |
          v
tp_combine_results       (briefing map with summary)
```

## Worker: PlanTools (`tp_plan_tools`)

Analyzes the user request and location to configure three parallel tool calls. Produces `toolConfigs` containing a weather config (`units: "fahrenheit"`, `includeHourly: true`), news topics (`["technology", "business", "world"]`), and stock tickers. Returns `toolCount: 3` and the list of planned tools.

## Worker: CallWeather (`tp_call_weather`)

Makes real HTTP calls to the Open-Meteo API. First geocodes the location via `geocoding-api.open-meteo.com/v1/search`. Then fetches current weather and hourly forecast via `api.open-meteo.com/v1/forecast` with parameters for temperature, wind speed, weather code, and 4-hour hourly data. Maps WMO weather codes to condition strings. Uses `HttpClient` with a 10-second timeout. On network failure, returns an error map instead of crashing.

## Worker: CallNews (`tp_call_news`)

Fetches real headlines using the Wikipedia API. For each topic, performs a Wikipedia opensearch (`en.wikipedia.org/w/api.php?action=opensearch`). Also fetches the `Portal:Current_events` summary for a general overview. Returns a list of headline maps with `title` and `source` fields. No API key required.

## Worker: CallStocks (`tp_call_stocks`)

Fetches real stock data from Yahoo Finance. For each ticker, calls `query2.finance.yahoo.com/v8/finance/chart/{ticker}?range=1d&interval=1d`. Parses `regularMarketPrice` and `chartPreviousClose` from the chart metadata, then computes `change` and `changePercent`. Errors are handled per-ticker so one failure does not block others.

## Worker: CombineResults (`tp_combine_results`)

Merges weather, news, and stocks into a `briefing` map with null-safe access (`weather != null ? weather : Map.of()`). Adds a human-readable `summary` string covering the morning conditions, top headlines, and market movements. Returns `toolsUsed: ["weather_api", "news_api", "stock_api"]` and `generatedAt` timestamp.

## Tests

5 tests cover tool planning, all three parallel API calls, and result combination.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
