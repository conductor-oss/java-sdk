# Tool Use Caching in Java Using Conductor : Check Cache, Execute-or-Return-Cached, Store Results

Tool Use Caching. checks a cache before executing a tool, and caches the result afterward. Uses a SWITCH task to branch on cache hit vs miss. ## Tool Calls Are Expensive : Don't Repeat Them

Tool calls cost time and often money. A web search API charges per query. A database query consumes compute resources. A calculation takes CPU time. If the same tool is called with the same arguments within a short window (same weather query, same stock lookup, same calculation), returning the cached result saves time and cost.

The caching pattern checks the cache before executing: if the tool name and arguments match a recent result (within the TTL), return it immediately. Otherwise, execute the tool, cache the result with the configured TTL, and return it. The `SWITCH` task makes this routing explicit. cache hits skip execution entirely, and every request records whether it was served from cache or computed fresh.

## The Solution

**You write the cache lookup, tool execution, and cache storage logic. Conductor handles the hit/miss routing, TTL management, and cache hit-rate tracking.**

`CheckCacheWorker` looks up the tool name and arguments in the cache and returns hit/miss status with any cached result. Conductor's `SWITCH` routes on cache status: hits go to `ReturnCachedWorker` which formats and returns the cached result. Misses go to `ExecuteToolWorker` which runs the tool, then `CacheResultWorker` which stores the result with the specified TTL. Conductor records cache hit rates per tool, enabling you to tune TTL values based on actual usage patterns.

### What You Write: Workers

Four workers implement caching. Checking the cache for a prior result, routing cache hits to direct return, and routing misses through execution and storage.

| Worker | Task | What It Does |
|---|---|---|
| **CacheResultWorker** | `uc_cache_result` | Stores the tool execution result in the cache. Input fields: cacheKey, result, ttlSeconds. Output fields: storedResul... |
| **CheckCacheWorker** | `uc_check_cache` | Checks the cache for a previously computed tool result. Computes a cacheKey from toolName + toolArgs and simulates a ... |
| **ExecuteToolWorker** | `uc_execute_tool` | Executes the requested tool and returns its result. Simulates a currency conversion: converts an amount from one curr... |
| **ReturnCachedWorker** | `uc_return_cached` | Returns a previously cached result directly. Input fields: cachedResult, cacheKey, cachedAt. Output fields: result, f... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
uc_check_cache
 │
 ▼
SWITCH (cache_decision_ref)
 ├── hit: uc_return_cached
 └── default: uc_execute_tool -> uc_cache_result

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
