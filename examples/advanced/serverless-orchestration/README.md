# Serverless Function Chain in Java Using Conductor : Parse, Enrich, Score, Aggregate

## Chaining Lambda Functions Without Losing Control

You have four Lambda functions that need to run in sequence: one parses raw event payloads into structured data, one enriches the parsed data with external API lookups, one scores the enriched records (fraud score, relevance score, risk score), and one aggregates the scored results into a summary. Each function's output is the next function's input.

Chaining Lambdas natively (Step Functions, direct invocation) works until you need retries with backoff on the enrichment call, timeout handling on the scoring function, and a complete trace of which event produced which score. You end up building orchestration logic inside your Lambda code, defeating the purpose of serverless.

## The Solution

**You write each function's invocation logic. Conductor handles the chain, cold-start retries, and end-to-end tracing.**

`SvlInvokeParseWorker` calls the parse function to extract structured fields from the raw event payload. `SvlInvokeEnrichWorker` calls the enrichment function to augment the parsed data with external context. `SvlInvokeScoreWorker` calls the scoring function to compute a score on the enriched data. `SvlAggregateWorker` combines the scored results into a final summary. Conductor chains these invocations, retries any that fail (cold start timeouts, transient API errors), and records the full input/output of each function call.

### What You Write: Workers

Four workers chain serverless invocations: event parsing, external enrichment, scoring computation, and result aggregation, each wrapping one Lambda function call with retry-safe orchestration.

| Worker | Task | What It Does |
|---|---|---|
| **SvlAggregateWorker** | `svl_aggregate` | Aggregates results from the serverless function chain. |
| **SvlInvokeEnrichWorker** | `svl_invoke_enrich` | Invokes the enrich serverless function to add user context. |
| **SvlInvokeParseWorker** | `svl_invoke_parse` | Invokes the parse serverless function for an incoming event. |
| **SvlInvokeScoreWorker** | `svl_invoke_score` | Invokes the score serverless function to compute engagement score.### The Workflow

```
svl_invoke_parse
 │
 ▼
svl_invoke_enrich
 │
 ▼
svl_invoke_score
 │
 ▼
svl_aggregate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
