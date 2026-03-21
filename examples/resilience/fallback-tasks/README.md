# Implementing Fallback Tasks in Java with Conductor : Primary API, Secondary API, and Cache Lookup

## The Problem

You depend on an external API for critical data, but it's not always available. You need a tiered fallback strategy: try the primary API first, fall back to a secondary API (different provider, different region) if the primary is down, and serve cached/stale data as a last resort. The user should always get a response. the quality may degrade, but the system never returns an error.

Without orchestration, fallback logic nests into deeply indented try/catch chains. The secondary API call is buried inside the primary's catch block, and the cache lookup is inside the secondary's catch block. Each layer adds complexity, and it's impossible to tell at a glance which data source actually served a given request.

## The Solution

**You just write the primary, secondary, and cache lookup logic. Conductor handles SWITCH-based fallback routing through the tiered chain, retries at each level, and a record of every request showing which data source ultimately served the response.**

The primary API worker makes the call. Based on its result, Conductor's SWITCH task routes to either the response path (primary succeeded), the secondary API (primary failed), or the cache lookup (both failed). Each fallback level is a simple, independent worker. Every request is tracked. you can see which data source served each response and how far down the fallback chain it went. ### What You Write: Workers

PrimaryApiWorker tries the preferred data source first, SecondaryApiWorker serves as the backup provider if the primary is unavailable, and CacheLookupWorker delivers stale-but-valid cached data as the last resort.

| Worker | Task | What It Does |
|---|---|---|
| **CacheLookupWorker** | `fb_cache_lookup` | Cache lookup worker (fb_cache_lookup). Last-resort fallback using cached data. Always succeeds. Returns: source = ".. |
| **PrimaryApiWorker** | `fb_primary_api` | Primary API worker (fb_primary_api). Calls the primary data source. |
| **SecondaryApiWorker** | `fb_secondary_api` | Secondary API worker (fb_secondary_api). Fallback API data source. Always succeeds. Returns: source = "secondary" d.. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
fb_primary_api
 │
 ▼
SWITCH (fallback_switch_ref)
 ├── unavailable: fb_secondary_api
 ├── error: fb_cache_lookup

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
