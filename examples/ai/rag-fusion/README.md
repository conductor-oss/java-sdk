# RAG Fusion: Multi-Query Search with Reciprocal Rank Fusion (k=60)

One query misses relevant documents that a rephrased version would find. This pipeline rewrites the question into variants, searches three indices in parallel via FORK_JOIN, fuses results using RRF with k=60 (`score = sum(1/(k+rank))`), and generates from the top fused documents.

## Workflow

```
question
    │
    ▼
┌──────────────────────┐
│ rf_rewrite_queries   │  Generate query variants
└───────────┬──────────┘
            ▼
┌─── FORK_JOIN ──────────────────────────────────┐
│ ┌──────────────┐ ┌──────────────┐ ┌───────────┐│
│ │rf_search_v1  │ │rf_search_v2  │ │rf_search_v3││
│ └──────────────┘ └──────────────┘ └───────────┘│
└──────────────────────┬─────────────────────────┘
                       ▼
            ┌─────────────────────┐
            │ rf_fuse_results     │  RRF: score = sum(1/(60+rank))
            └──────────┬──────────┘
                       ▼
            ┌─────────────────────┐
            │ rf_generate_answer  │  Generate from fused context
            └─────────────────────┘
```

## Workers

**RewriteQueriesWorker** (`rf_rewrite_queries`) -- Generates query variants. When API key is set, uses LLM to rewrite.

**SearchV1Worker** (`rf_search_v1`) -- Returns docs about versioning and retry policies with rank positions.

**SearchV2Worker** (`rf_search_v2`) -- Returns docs about JSON data flow and versioning (overlaps with V1 on versioning doc).

**SearchV3Worker** (`rf_search_v3`) -- Returns docs about polyglot workers and visual editors.

**FuseResultsWorker** (`rf_fuse_results`) -- Implements RRF with `k=60`. For each document ID across all result lists, sums `1/(k + rank)`. Handles null result lists gracefully. Returns `fusedDocs` sorted descending, `fusedCount`, and `totalCandidates`.

**GenerateAnswerWorker** (`rf_generate_answer`) -- Generates from the top fused documents.

## Tests

41 tests extensively cover query rewriting, all three searches, RRF fusion math, and generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
