# Parallel Search Across API Docs, Tutorials, and Forums

A user's question might be answered in the API reference, a tutorial, or a community forum post. Searching them sequentially is slow. This workflow embeds the query, then uses a FORK_JOIN to search all three sources in parallel, merges the results, and generates a unified answer.

## Workflow

```
question
    │
    ▼
┌──────────────┐
│ mdrag_embed  │  Embed query via OpenAI text-embedding-3-small
└──────┬───────┘
       │
       ▼
┌─── FORK_JOIN ──────────────────────────────────────────┐
│                        │                               │
│  ┌─────────────────┐  │  ┌──────────────────────┐     │  ┌───────────────────┐
│  │mdrag_search_    │  │  │mdrag_search_         │     │  │mdrag_search_      │
│  │  api_docs       │  │  │  tutorials           │     │  │  forums           │
│  └─────────────────┘  │  └──────────────────────┘     │  └───────────────────┘
└────────────────────┬──┴───────────────┬───────────────┘
                     ▼                  ▼
               ┌──────────┐
               │   JOIN   │
               └────┬─────┘
                    ▼
           ┌────────────────────┐
           │ mdrag_merge_results│  Combine results from all sources
           └────────┬───────────┘
                    ▼
           ┌────────────────────┐
           │ mdrag_generate     │  Generate unified answer
           └────────────────────┘
                    │
                    ▼
              answer, sources
```

## Workers

**EmbedWorker** (`mdrag_embed`) -- When `CONDUCTOR_OPENAI_API_KEY` is set, calls `https://api.openai.com/v1/embeddings` with model `text-embedding-3-small`. Otherwise returns a deterministic vector.

**SearchApiDocsWorker** (`mdrag_search_api_docs`) -- Returns 2 hardcoded results about the `/workflow` endpoint (POST with JSON body) and task definition registration (`POST /metadata/taskdefs`), with relevance scores.

**SearchTutorialsWorker** (`mdrag_search_tutorials`) -- Returns 2 results: building a first Conductor workflow with Node.js workers in 10 minutes, and using FORK/JOIN for parallel task execution.

**SearchForumsWorker** (`mdrag_search_forums`) -- Returns a community answer about Conductor's automatic retry handling when workers fail.

**MergeResultsWorker** (`mdrag_merge_results`) -- Combines results from all three sources into a single list with source counts tracked in a `Map.of(...)`.

**GenerateWorker** (`mdrag_generate`) -- When `CONDUCTOR_OPENAI_API_KEY` is set, calls `gpt-4o-mini` with the merged context. Otherwise returns a deterministic answer.

## Tests

15 tests cover embedding, each search source, result merging, and answer generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
