# Production RAG with Caching, Rate Limiting, Token Budgets, and Audit Logging

A regulated enterprise needs a RAG pipeline that goes beyond "retrieve and generate." Every query must be checked against a cache, rate-limited per user, budgeted for token spend, and logged for SOC2 compliance. If the cache hits, the answer is served directly and the audit log records `source: "cache"` with zero tokens used. Otherwise, the full pipeline runs: rate check, retrieval, token budget trimming, generation, result caching, and audit logging.

## Workflow

```
question, userId
       │
       ▼
┌─────────────────┐
│ er_check_cache  │  Lookup by question hash
└────────┬────────┘
         │  cacheStatus ("hit" or "miss")
         ▼
    ┌─ SWITCH ────────────────────────────────────────┐
    │                                                 │
  "hit"                                         default (miss)
    │                                                 │
    ▼                                                 ▼
┌──────────────┐                             ┌────────────────┐
│ er_audit_log │                             │ er_rate_limit  │
│ (source:cache)│                            └───────┬────────┘
└──────────────┘                                     ▼
                                             ┌────────────────┐
                                             │ er_retrieve    │
                                             └───────┬────────┘
                                                     ▼
                                             ┌────────────────┐
                                             │ er_token_budget│
                                             └───────┬────────┘
                                                     ▼
                                             ┌────────────────┐
                                             │ er_generate    │
                                             └───────┬────────┘
                                                     ▼
                                             ┌────────────────┐
                                             │ er_cache_result│
                                             └───────┬────────┘
                                                     ▼
                                             ┌──────────────────┐
                                             │ er_audit_log     │
                                             │ (source:generated)│
                                             └──────────────────┘
```

## Workers

**CheckCacheWorker** (`er_check_cache`) -- Always returns `cacheStatus: "miss"` and `cachedAnswer: null` (demonstrates the cache-miss path). In production, this would query Redis or Memcached.

**RateLimitWorker** (`er_rate_limit`) -- Reports `allowed: true` with `current: 12`, `limit: 60`, `remaining: 48` for the given `userId`.

**RetrieveWorker** (`er_retrieve`) -- Returns 4 hardcoded context documents with token counts: `doc-101` (142 tokens, RAG explanation), `doc-202` (118, vector databases), `doc-303` (95, token budgets), `doc-404` (87, caching strategies).

**TokenBudgetWorker** (`er_token_budget`) -- Enforces a `dailyBudget` of 50,000 tokens with `usedToday: 12,000`. Iterates the context list, summing each document's `tokens` field (cast from `Number` to `int`). Returns `trimmedContext`, `remainingBudget: 38000`, and the total `contextTokens`.

**GenerateWorker** (`er_generate`) -- When `CONDUCTOR_OPENAI_API_KEY` is set, calls `gpt-4o-mini` with `temperature: 0.3` and a max_tokens capped by `Math.min(tokenBudget, 512)`. In fallback mode, returns a 187-token answer about RAG with `model: "gpt-4o"` and `latencyMs: 820`. Estimates tokens as `answer.split("\\s+").length * 2`.

**CacheResultWorker** (`er_cache_result`) -- Computes `cacheKey` as `"rag:" + question.hashCode()`. Sets a TTL of 3600 seconds (1 hour) and calculates `expiresAt` via `Instant.now().plusSeconds(ttlSeconds)`.

**AuditLogWorker** (`er_audit_log`) -- Creates a SOC2-compliant audit entry with `Instant.now()` timestamp, userId, question, sessionId, source, answer, tokensUsed, and `compliance: "SOC2-logged"`. Null-safe on all fields (falls back to `"unknown"` or empty string).

## Tests

21 tests cover all 7 workers including cache lookup, rate limiting, token budget enforcement, and audit logging.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
