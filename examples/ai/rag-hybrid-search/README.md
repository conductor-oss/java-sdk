# Hybrid Search: Vector + BM25 Keyword with Reciprocal Rank Fusion

Pure vector search misses exact error codes. Pure keyword search misses semantic synonyms. This pipeline runs both in parallel via FORK_JOIN against a shared 6-document corpus about Conductor, merges them with RRF, and generates an answer.

## Workflow

```
question
    │
    ▼
┌─── FORK_JOIN ────────────────────────────────┐
│ ┌──────────────────────┐ ┌──────────────────┐│
│ │hs_vector_search      │ │hs_keyword_search ││
│ │(Jaccard similarity)  │ │(real BM25 scores)││
│ └──────────────────────┘ └──────────────────┘│
└──────────────────┬───────────────────────────┘
                   ▼
         ┌──────────────────┐
         │ hs_rrf_merge     │  Deduplicate + RRF
         └────────┬─────────┘
                  ▼
         ┌──────────────────┐
         │ hs_generate_answer│
         └──────────────────┘
```

## Workers

**VectorSearchWorker** (`hs_vector_search`) -- Searches a bundled `DOCUMENTS` list (6 docs: orchestration, worker polling, dynamic fork, task domains, sub-workflows, event handlers) using Jaccard token similarity.

**KeywordSearchWorker** (`hs_keyword_search`) -- Tokenizes the query and computes real BM25 scores against the same document set. Returns ranked results.

**RrfMergeWorker** (`hs_rrf_merge`) -- Merges results from both searches, deduplicating by ID via `seen.contains(id)`. Applies RRF scoring.

**GenerateAnswerWorker** (`hs_generate_answer`) -- Generates from merged context. Estimates tokens via `answer.split("\\s+").length`.

## Tests

18 tests cover vector search, BM25 scoring, RRF merging, and generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
