# Caching LLM Responses to Avoid Paying for the Same Answer Twice

LLM calls cost $0.01-$0.10+ each and take 1-10 seconds. When the same support question or product description request comes in repeatedly, every duplicate prompt makes a full API round-trip. This workflow hashes the prompt into a cache key, checks a `ConcurrentHashMap`-backed cache, and tracks cost savings.

## Workflow

```
prompt, model
     │
     ▼
┌────────────────────┐
│ cache_hash_prompt  │  Build cache key from model + prompt
└─────────┬──────────┘
          │  cacheKey
          ▼
┌────────────────────┐
│ cache_llm_call     │  Check cache, call LLM on miss
└─────────┬──────────┘
          │  response, cacheHit, latencyMs
          ▼
┌────────────────────┐
│ cache_report       │  Report savings
└────────────────────┘
          │
          ▼
   cacheKey, response, cacheHit, latencyMs, saved
```

## Workers

**CacheHashPromptWorker** (`cache_hash_prompt`) -- Concatenates `model + ":" + prompt`, replaces all whitespace with `"_"` via `replaceAll("\\s+", "_")`, and truncates to 64 characters. The result becomes the `cacheKey`.

**CacheLlmCallWorker** (`cache_llm_call`) -- Uses a static `ConcurrentHashMap<String, String>` called `CACHE`. On cache hit, returns the stored response with `cacheHit: true` and `latencyMs: 0`. On cache miss with `CONDUCTOR_OPENAI_API_KEY` set, calls `gpt-4o-mini` Chat Completions, measures wall-clock latency via `System.currentTimeMillis()`, stores the result in `CACHE`, and returns `cacheHit: false`. In fallback mode, stores a fixed response about Conductor's durable workflow execution with `latencyMs: 850`.

**CacheReportWorker** (`cache_report`) -- Checks `Boolean.TRUE.equals(cacheHitObj)`. On cache hit, reports `"Yes -- saved ~$0.02"`. On cache miss, reports `"No -- first request"`.

## Tests

7 tests cover prompt hashing, cache hit/miss behavior, and savings reporting.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
