# Semi-Structured RAG: Parallel Search Across Tables and Documents

When the answer lives in both a database table (revenue: $4.2M, match: 0.95) and a text chunk (chunk-001, score: 0.91), you need to search both in parallel. This pipeline classifies the data into structured fields and unstructured text, searches both via FORK_JOIN, merges the results, and generates a unified answer.

## Workflow

```
question, dataContext
       │
       ▼
┌────────────────────────┐
│ ss_classify_data       │  Identify structured fields + unstructured text
└───────────┬────────────┘
            ▼
┌─── FORK_JOIN ──────────────────────────────────────┐
│ ┌────────────────────────┐ ┌──────────────────────┐│
│ │ss_search_structured    │ │ss_search_unstructured││
│ │(field/value/table)     │ │(chunkId/score/snippet)││
│ └────────────────────────┘ └──────────────────────┘│
└──────────────────────┬─────────────────────────────┘
                       ▼
            ┌────────────────────┐
            │ ss_merge_results   │  Combine both result types
            └────────┬───────────┘
                     ▼
            ┌────────────────────┐
            │ ss_generate        │  Generate from merged context
            └────────────────────┘
```

## Workers

**ClassifyDataWorker** (`ss_classify_data`) -- Returns structured fields: `{field: "revenue", type: "numeric", source: "financials_db"}`, `{field: "employee_count", type: "numeric", source: "hr_db"}`.

**SearchStructuredWorker** (`ss_search_structured`) -- Returns `{field: "revenue", value: "$4.2M", table: "financials_db", match: 0.95}` and more.

**SearchUnstructuredWorker** (`ss_search_unstructured`) -- Returns `{chunkId: "chunk-001", score: 0.91, snippet: "..."}`.

**MergeResultsWorker** (`ss_merge_results`) -- Combines structured and unstructured results, appending scores and sources.

**GenerateWorker** (`ss_generate`) -- Parses context by splitting on newlines. Generates from the combined context.

## Tests

35 tests cover data classification, both search types, result merging, and generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
