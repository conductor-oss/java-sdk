# LLM Caching in Java Using Conductor : Hash Prompts, Cache Responses, Track Savings

## Paying for the Same Answer Twice

LLM API calls are slow (1-10 seconds) and expensive ($0.01-$0.10+ per call). In production, many prompts are repeated. the same support question, the same product description request, the same summarization of a document that hasn't changed. Without caching, every duplicate prompt makes a full round-trip to the LLM API, burning tokens and adding latency.

The caching pipeline needs three steps: hash the prompt (with the model name) to create a stable cache key, check the cache and either return the cached response or call the LLM and store the result, then report whether it was a hit or miss so you can track savings over time. If the LLM call fails, you need to retry it without re-hashing. the cache key is still valid. And you want visibility into cache hit rates and estimated cost savings across all calls.

## The Solution

**You write the prompt hashing, cache lookup, and LLM call logic. Conductor handles the cache-aware pipeline, retries, and observability.**

Each concern is an independent worker. prompt hashing, cache-aware LLM invocation, savings reporting. Conductor chains them so the cache key feeds into the LLM call worker (which checks the cache first), and the hit/miss result feeds into the reporter. If the LLM API times out on a cache miss, Conductor retries automatically. Every call records whether it was a cache hit or miss, the response latency, and the estimated cost savings.

### What You Write: Workers

Three workers implement the caching layer. hashing the prompt and model into a deterministic cache key, performing a cache-aware LLM call that returns cached responses on hit, and reporting cache hit rates with estimated cost savings.

| Worker | Task | What It Does |
|---|---|---|
| **CacheHashPromptWorker** | `cache_hash_prompt` | Creates a deterministic cache key by concatenating model and prompt, normalizing whitespace, and truncating to 64 characters | Processing only |
| **CacheLlmCallWorker** | `cache_llm_call` | Checks an in-memory cache for the key. on hit returns the cached response instantly, on miss calls OpenAI API (live) or returns a fixed response (demo), stores the result, and reports latency |
| **CacheReportWorker** | `cache_report` | Reports whether the call was a cache hit or miss and estimates cost savings (~$0.02 per cache hit) | Processing only |

**Live vs Demo mode:** When `CONDUCTOR_OPENAI_API_KEY` is set, `CacheLlmCallWorker` calls the OpenAI Chat Completions API (model: `gpt-4o-mini`) on cache miss. Without the key, it runs in demo mode with deterministic output prefixed with `[DEMO]`. Non-LLM workers (hashing, reporting) always run their real logic.

### The Workflow

```
cache_hash_prompt
 │
 ▼
cache_llm_call
 │
 ▼
cache_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
