# Multi-Document RAG in Java Using Conductor : Parallel Search Across API Docs, Tutorials, and Forums

## Answers That Span Multiple Knowledge Sources

A developer question like "How do I paginate API results?" might have the answer spread across three places: the API reference (parameter names and types), a tutorial (step-by-step walkthrough), and a forum thread (common gotchas and workarounds). Searching a single collection misses context. Searching all three sequentially triples the latency.

The solution is parallel search: embed the question once, then search API docs, tutorials, and forums simultaneously. After all three searches complete, merge the results by relevance score. giving priority to API docs for technical accuracy while including tutorial context and forum insights. The merged context feeds into the LLM for a comprehensive answer that cites multiple source types.

Without orchestration, parallel search means managing thread pools, handling partial failures (forums are down but API docs responded), and waiting for all results before merging. code that's hard to get right and impossible to observe.

## The Solution

**You write the per-collection search and merge logic. Conductor handles the parallel execution, retries, and observability.**

Each search is an independent worker. one per collection. Conductor's `FORK_JOIN` runs all three in parallel and waits for all to complete. A merge worker combines the results, and a generation worker produces the answer. If the forum search times out, Conductor retries it independently without re-running the API docs or tutorial searches.

### What You Write: Workers

Six workers implement cross-collection RAG. embedding the query, then searching API docs, tutorials, and forums in parallel via FORK_JOIN, merging the ranked results, and generating an answer from the unified context.

| Worker | Task | What It Does |
|---|---|---|
| **EmbedWorker** | `mdrag_embed` | Worker that generates a fixed embedding vector for a query. |
| **GenerateWorker** | `mdrag_generate` | Worker that generates an answer from the merged context. |
| **MergeResultsWorker** | `mdrag_merge_results` | Worker that merges results from api_docs, tutorials, and forums collections, sorts by score descending, and returns m... |
| **SearchApiDocsWorker** | `mdrag_search_api_docs` | Worker that searches the API docs collection and returns 2 results. |
| **SearchForumsWorker** | `mdrag_search_forums` | Worker that searches the forums collection and returns 1 result. |
| **SearchTutorialsWorker** | `mdrag_search_tutorials` | Worker that searches the tutorials collection and returns 2 results. |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
mdrag_embed
 │
 ▼
FORK_JOIN
 ├── mdrag_search_api_docs
 ├── mdrag_search_tutorials
 └── mdrag_search_forums
 │
 ▼
JOIN (wait for all branches)
mdrag_merge_results
 │
 ▼
mdrag_generate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
