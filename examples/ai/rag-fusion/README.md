# RAG Fusion in Java Using Conductor : Multi-Query Parallel Search with Reciprocal Rank Fusion

## Why One Query Is Not Enough

A single search query captures one perspective on the user's question. Rephrasing the same question in different ways. more specific, more general, using synonyms, from different angles, retrieves different relevant documents. RAG Fusion rewrites the original question into multiple queries, searches with each one in parallel, and merges the results using reciprocal rank fusion, which gives higher scores to documents that appear in multiple result sets.

The parallel search step is critical: three sequential searches take 3x the latency, but three parallel searches take 1x. After fusion, the merged results feed into generation for a more comprehensive answer.

## The Solution

**You write the query rewriting, parallel search, and rank fusion logic. Conductor handles the parallel execution, retries, and observability.**

A query rewriter generates multiple query variants. Conductor's `FORK_JOIN` searches with all three in parallel. A fusion worker applies reciprocal rank fusion to merge and re-rank the results. A generation worker produces the answer from the fused context. If any individual search times out, Conductor retries it independently.

### What You Write: Workers

Six workers implement RAG fusion. rewriting the original query into multiple variants, running three parallel searches via FORK_JOIN, fusing the results with Reciprocal Rank Fusion, and generating an answer from the combined context.

| Worker | Task | What It Does |
|---|---|---|
| **FuseResultsWorker** | `rf_fuse_results` | Worker that fuses results from multiple search engines using Reciprocal Rank Fusion (RRF). Uses k=60, score = sum(1/(... |
| **GenerateAnswerWorker** | `rf_generate_answer` | Worker that generates an answer using the original question and fused context from multiple search engines. Combines ... |
| **RewriteQueriesWorker** | `rf_rewrite_queries` | Worker that rewrites the original question into 3 variant queries for multi-perspective retrieval. Returns determinis... |
| **SearchV1Worker** | `rf_search_v1` | Search engine V1 worker. Takes a query and variantIndex, returns ranked results with id, text, and rank. Simulates a ... |
| **SearchV2Worker** | `rf_search_v2` | Search engine V2 worker. Takes a query and variantIndex, returns ranked results with id, text, and rank. Simulates a ... |
| **SearchV3Worker** | `rf_search_v3` | Search engine V3 worker. Takes a query and variantIndex, returns ranked results with id, text, and rank. Simulates a ... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
rf_rewrite_queries
 │
 ▼
FORK_JOIN
 ├── rf_search_v1
 ├── rf_search_v2
 └── rf_search_v3
 │
 ▼
JOIN (wait for all branches)
rf_fuse_results
 │
 ▼
rf_generate_answer

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
