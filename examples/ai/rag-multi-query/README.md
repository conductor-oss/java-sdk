# Multi-Query RAG: Expand One Question into Three Searches in Parallel

A single query gives you one perspective. This pipeline expands the question into 3 query variants, searches for each in parallel via FORK_JOIN, deduplicates the results (documents like "d1" and "d4" appear in multiple result sets), and generates from the combined unique context.

## Workflow

```
question
    │
    ▼
┌──────────────────────┐
│ mq_expand_queries    │  Generate 3 query variants
└───────────┬──────────┘
            ▼
┌─── FORK_JOIN ──────────────────────────────┐
│ ┌──────────────┐ ┌────────────┐ ┌─────────┐│
│ │mq_search_q1  │ │mq_search_q2│ │mq_search││
│ │              │ │            │ │  _q3    ││
│ └──────────────┘ └────────────┘ └─────────┘│
└──────────────────────┬─────────────────────┘
                       ▼
            ┌──────────────────────┐
            │ mq_dedup_results     │  Remove duplicate doc IDs
            └──────────┬───────────┘
                       ▼
            ┌──────────────────────┐
            │ mq_generate_answer   │  Generate from unique docs
            └──────────────────────┘
```

## Workers

**ExpandQueriesWorker** (`mq_expand_queries`) -- Generates 3 query variants. When API key is set, uses LLM for expansion.

**SearchQ1Worker** (`mq_search_q1`) -- Returns docs `d1` (centralized control) and `d4` (retry/timeout logic).

**SearchQ2Worker** (`mq_search_q2`) -- Returns `d1` (same as Q1), `d7` (decoupled execution).

**SearchQ3Worker** (`mq_search_q3`) -- Returns `d4` (same as Q1), `d11` (choreography hidden dependencies).

**DedupResultsWorker** (`mq_dedup_results`) -- Merges all three result lists (handling null lists gracefully) and removes duplicates, keeping unique documents.

**GenerateAnswerWorker** (`mq_generate_answer`) -- Generates from deduplicated context. Estimates tokens via `answer.split("\\s+").length`.

## Tests

27 tests cover query expansion, all three searches, deduplication logic, and answer generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
