# Cached Tool Calls: Check Cache, SWITCH on Hit/Miss, Store Result

Before executing, the cache is checked (always returns `cacheStatus: "miss"` in the demo). A SWITCH routes: on hit, return the cached result (`fromCache: true`). On miss, execute the tool (e.g., currency conversion with `baseCurrency` from args), then store the result (`cached: true`) for future lookups.

## Workflow

```
toolName, toolArgs, cacheTtlSeconds
  -> uc_check_cache -> SWITCH(hit: uc_return_cached, miss: uc_execute_tool -> uc_cache_result)
```

## Tests

36 tests cover cache hit, cache miss with execution, and result storage.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
