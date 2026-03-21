# Enterprise RAG in Java Using Conductor : Caching, Rate Limiting, Token Budgets, and Audit Logging

## Beyond the Demo: What Production RAG Actually Requires

A basic RAG pipeline (retrieve, generate) works fine in a demo. In production, it falls apart. Without caching, identical questions hit the LLM repeatedly. burning tokens and adding latency. Without rate limiting, a single user (or a bug in a client) can exhaust your API budget in minutes. Without token budgets, a query that retrieves too much context overflows the model's context window and fails. And without audit logging, you can't answer "what did user X ask, and what did we tell them?", a compliance requirement in regulated industries.

These cross-cutting concerns create branching logic: if the cache hits, skip retrieval and generation entirely but still log the query. If the rate limit is exceeded, reject the request before doing any work. If the token budget is tight, trim the retrieved context before sending it to the LLM. Each concern interacts with the others, and all of them need independent error handling.

Without orchestration, you'd layer these checks into a single method with nested conditionals, manual cache lookups, and scattered logging. code that's nearly impossible to test, debug, or modify when compliance requirements change.

## The Solution

**You write the caching, rate-limiting, token budgeting, and audit logging logic. Conductor handles the conditional routing, retries, and observability.**

Each concern is an independent worker. cache check, rate limiting, retrieval, token budget enforcement, generation, cache storage, audit logging. Conductor's `SWITCH` task skips the entire generation path on cache hits. If the rate limiter rejects a request, the pipeline stops cleanly. Every query is audit-logged regardless of which path it took, and every execution is tracked with full inputs and outputs for compliance review.

### What You Write: Workers

Seven workers implement enterprise guardrails around a RAG core. cache check, rate limiting, retrieval, token budget enforcement, generation, cache storage, and audit logging, with a SWITCH that skips generation entirely on cache hits.

| Worker | Task | What It Does |
|---|---|---|
| **AuditLogWorker** | `er_audit_log` | Worker that creates a audit-ready pattern audit log entry for every query. Takes userId, question, sessionId, source, answ... |
| **CacheResultWorker** | `er_cache_result` | Worker that caches a generated answer for future lookups. Takes question, answer, and ttlSeconds. Returns cached stat... |
| **CheckCacheWorker** | `er_check_cache` | Worker that checks a cache for a previously answered question. Takes question and userId, returns cacheStatus and cac... |
| **GenerateWorker** | `er_generate` | Worker that generates an answer using an LLM given question, context, and token budget. Returns the answer, tokensUse... |
| **RateLimitWorker** | `er_rate_limit` | Worker that checks rate limits for a user. Takes userId, returns allowed status and rate limit details. |
| **RetrieveWorker** | `er_retrieve` | Worker that retrieves relevant context documents for a question. Returns 4 context documents with id, text, and token... |
| **TokenBudgetWorker** | `er_token_budget` | Worker that manages token budgets by trimming context to stay within limits. Takes context and userId, returns trimme... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
er_check_cache
 │
 ▼
SWITCH (cache_decision_ref)
 ├── hit: er_audit_log
 └── default: er_rate_limit -> er_retrieve -> er_token_budget -> er_generate -> er_cache_result -> er_audit_log

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
