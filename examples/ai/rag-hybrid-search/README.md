# RAG Hybrid Search in Java Using Conductor: Vector + Keyword Search with Reciprocal Rank Fusion

Pure vector search returns documents about "network connectivity issues" when the user searched for the exact error code `ERR_CONNECTION_REFUSED`. Semantically similar, factually useless. Pure keyword search finds the error code but misses the doc titled "Troubleshooting refused connections" because it uses different words. Neither search strategy alone is good enough, and running both sequentially doubles your latency. This example builds a hybrid search pipeline using [Conductor](https://github.com/conductor-oss/conductor) that runs vector and BM25 keyword search in parallel, fuses the results with Reciprocal Rank Fusion, and generates an answer from the combined context.

## Neither Vector Nor Keyword Search Is Enough Alone

Vector search understands meaning but misses exact terms. searching for "ERR_CONNECTION_REFUSED" by semantic similarity might return documents about network errors in general, not the specific error code. Keyword search finds exact matches but misses synonyms, searching for "car insurance" won't find documents about "automobile coverage." Hybrid search runs both in parallel and combines the results.

Reciprocal rank fusion (RRF) merges the two ranked lists by giving each document a score based on its position in each list. Documents that rank highly in both searches float to the top.

## The Solution

**You write the vector search, keyword search, and RRF merge logic. Conductor handles the parallel execution, retries, and observability.**

Vector search and keyword search are independent workers. Conductor's `FORK_JOIN` runs both in parallel. An RRF merge worker combines the ranked results, and a generation worker produces the answer from the fused context. If the keyword index is slow, Conductor retries it without re-running the vector search.

### What You Write: Workers

Four workers implement hybrid search. Running vector similarity and BM25 keyword search in parallel via FORK_JOIN, merging results with Reciprocal Rank Fusion, and generating an answer from the fused context.

| Worker | Task | What It Does |
|---|---|---|
| **GenerateAnswerWorker** | `hs_generate_answer` | Answer generation worker. Generates an answer from the fused context documents. |
| **KeywordSearchWorker** | `hs_keyword_search` | Keyword (BM25) search worker. Simulates tokenizing the query and searching an inverted index. |
| **RrfMergeWorker** | `hs_rrf_merge` | Reciprocal Rank Fusion (RRF) merge worker. Deduplicates results from vector and keyword searches by document id, keep |
| **VectorSearchWorker** | `hs_vector_search` | Vector similarity search worker. Simulates embedding the query and searching an HNSW index (cosine similarity). |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode, the workflow and worker interfaces stay the same.

### The Workflow

```
FORK_JOIN
 ├── hs_vector_search
 └── hs_keyword_search
 │
 ▼
JOIN (wait for all branches)
hs_rrf_merge
 │
 ▼
hs_generate_answer

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
