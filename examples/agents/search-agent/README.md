# Search Agent in Java Using Conductor : Formulate Queries, Parallel Google/Wiki Search, Rank, Synthesize

Search Agent. formulate queries, search Google and Wikipedia in parallel, rank/merge results, and synthesize a final answer. ## Good Answers Need Multiple Search Sources

A question like "What are the environmental impacts of lithium mining?" benefits from both web search (current news, industry reports, environmental assessments) and Wikipedia (background knowledge, historical context, established science). Searching either alone gives incomplete results. Searching both sequentially doubles the latency.

Parallel search across Google and Wikipedia halves the wait time. But the results need merging: Google results have snippets and URLs, Wikipedia results have structured summaries and references. Rank-merging combines them by relevance, deduplicates overlapping information, and produces a single ranked result set. The synthesis step then generates a grounded answer with citations to both web and Wikipedia sources.

## The Solution

**You write the query formulation, search execution, ranking, and synthesis logic. Conductor handles parallel search dispatch, result merging, and source attribution.**

`FormulateQueriesWorker` analyzes the question and generates targeted search queries optimized for each source. keyword queries for Google, topic queries for Wikipedia. `FORK_JOIN` dispatches `SearchGoogleWorker` and `SearchWikiWorker` simultaneously. After `JOIN` collects both result sets, `RankMergeWorker` combines and deduplicates results by relevance score, normalizing across different source formats. `SynthesizeWorker` generates a comprehensive answer from the ranked results with inline citations to both web and Wikipedia sources. Conductor runs both searches in parallel and records the contribution of each source to the final answer.

### What You Write: Workers

Five workers run the search pipeline. Formulating queries, searching Google and Wikipedia in parallel, ranking and merging results, and synthesizing a grounded answer.

| Worker | Task | What It Does |
|---|---|---|
| **FormulateQueriesWorker** | `sa_formulate_queries` | Takes a user question and formulates optimized search queries. Returns a list of queries, the detected intent, and co... |
| **RankMergeWorker** | `sa_rank_merge` | Merges Google and Wikipedia results, sorts by relevance descending, and returns the ranked results along with top sou... |
| **SearchGoogleWorker** | `sa_search_google` | Simulates a Google search using the provided queries. Returns search results with title, url, snippet, relevance, and... |
| **SearchWikiWorker** | `sa_search_wiki` | Simulates a Wikipedia search using the provided queries. Returns search results with title, url, snippet, relevance, ... |
| **SynthesizeWorker** | `sa_synthesize` | Synthesizes a final answer from ranked search results and top sources. Produces a coherent answer string, confidence ... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
sa_formulate_queries
 │
 ▼
FORK_JOIN
 ├── sa_search_google
 └── sa_search_wiki
 │
 ▼
JOIN (wait for all branches)
sa_rank_merge
 │
 ▼
sa_synthesize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
