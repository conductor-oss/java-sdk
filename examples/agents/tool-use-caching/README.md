# Tool Use Caching: Check Cache, Branch on Hit/Miss, Store Results

An application repeatedly calls the same external API with identical arguments -- for instance, converting USD to EUR multiple times within minutes. Each redundant call wastes time and counts against the API's rate limit. Caching the result for a configurable TTL eliminates the duplicate work without sacrificing freshness.

This workflow checks a cache before executing a tool. A SWITCH task branches on `cacheStatus`: hits return immediately, misses execute the tool and store the result for future lookups.

## Pipeline Architecture

```
toolName, toolArgs, cacheTtlSeconds
         |
         v
  uc_check_cache         (cacheStatus, cacheKey, cachedResult)
         |
         v
  SWITCH on cacheStatus
     |              |
    hit           miss (default)
     |              |
     v              v
  uc_return      uc_execute_tool
  _cached           |
                    v
                 uc_cache_result
```

## Worker: CheckCache (`uc_check_cache`)

Computes a cache key by concatenating `toolName + ":" + toolArgs.toString()`. In this demonstration, the worker always returns `cacheStatus: "miss"` with `cachedResult: null` and `cachedAt: null`. The `reason` field explains: `"No cache entry exists for this key"`. Accepts `cacheTtlSeconds` as input (default 300) for downstream use.

## Worker: ReturnCached (`uc_return_cached`)

Activated on cache hit. Returns the `cachedResult` directly with `fromCache: true` and `cacheAge: "45s"` to indicate how stale the entry is.

## Worker: ExecuteTool (`uc_execute_tool`)

Performs a currency conversion: extracts `from`, `to`, and `amount` from `toolArgs` (defaults: USD, EUR, 1000.0). Applies a fixed exchange rate of `0.92` and computes `convertedAmount = amount * rate`. Returns a `LinkedHashMap` with `baseCurrency`, `targetCurrency`, `rate`, `amount`, `convertedAmount`, `provider: "exchange_rates_api"`, and `timestamp`. Reports `executionTimeMs: 312` and `apiCallMade: true`.

## Worker: CacheResult (`uc_cache_result`)

Stores the execution result under the cache key with the configured TTL. Returns `cached: true`, the `cacheKey`, `expiresAt: "2026-03-08T10:05:00Z"`, and the `ttlSeconds` that was applied.

## Tests

4 tests cover cache hit, cache miss with execution, result caching, and key computation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
