# Parallel Tool Calls: Weather, News, and Stocks via FORK_JOIN

The planner configures three tool calls: weather (with location config), news (topics: `["technology", "business", "world"]`), and stocks (with ticker list). All three execute in parallel via FORK_JOIN. The combiner merges into a `briefing` map (null-safe: `weather != null ? weather : Map.of()`).

## Workflow

```
userRequest, location
  -> tp_plan_tools -> FORK_JOIN(tp_call_weather | tp_call_news | tp_call_stocks) -> tp_combine_results
```

## Tests

42 tests cover planning, all three parallel tool calls, and result combination.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
