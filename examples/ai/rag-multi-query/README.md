# RAG Multi-Query in Java Using Conductor : Expand, Search in Parallel, Deduplicate, Generate

## One Question, Multiple Search Angles

A single vector search query captures one representation of the user's intent. By expanding the question into multiple variants. "How do I authenticate?" becomes "authentication setup guide", "login configuration steps", "auth token implementation", you retrieve documents that match different aspects of the same intent. The parallel searches return overlapping and complementary results that are deduplicated before generation.

## The Solution

**You write the query expansion, search, and deduplication logic. Conductor handles the parallel execution, retries, and observability.**

A query expander generates three variants. Conductor's `FORK_JOIN` searches with all three in parallel. A deduplication worker removes duplicate documents across result sets, and a generation worker produces the answer from the combined, deduplicated context.

### What You Write: Workers

Six workers implement multi-query retrieval. expanding the original question into three variant queries, searching for each in parallel via FORK_JOIN, deduplicating the combined results, and generating an answer from the broadened context.

| Worker | Task | What It Does |
|---|---|---|
| **DedupResultsWorker** | `mq_dedup_results` | Worker that deduplicates search results from multiple query branches. Takes results1, results2, results3 and returns ... |
| **ExpandQueriesWorker** | `mq_expand_queries` | Worker that expands a user question into multiple search query variants. |
| **GenerateAnswerWorker** | `mq_generate_answer` | Worker that generates a final answer from deduplicated context documents. |
| **SearchQ1Worker** | `mq_search_q1` | Worker that searches the knowledge base with query variant 1. Returns documents d1 and d4 (d1 overlaps with q2, d4 ov... |
| **SearchQ2Worker** | `mq_search_q2` | Worker that searches the knowledge base with query variant 2. Returns documents d1, d7, d9 (d1 overlaps with q1). |
| **SearchQ3Worker** | `mq_search_q3` | Worker that searches the knowledge base with query variant 3. Returns documents d4 and d11 (d4 overlaps with q1). |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
mq_expand_queries
 │
 ▼
FORK_JOIN
 ├── mq_search_q1
 ├── mq_search_q2
 └── mq_search_q3
 │
 ▼
JOIN (wait for all branches)
mq_dedup_results
 │
 ▼
mq_generate_answer

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
