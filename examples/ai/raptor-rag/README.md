# RAPTOR RAG in Java Using Conductor : Hierarchical Document Summarization Tree for Multi-Level Retrieval

## Beyond Flat Chunk Retrieval

Standard RAG retrieves individual chunks. fine-grained text fragments that match the query. But some questions need high-level understanding: "What are the main themes of this paper?" can't be answered by any single chunk. RAPTOR builds a tree of summaries: leaf nodes are chunk-level summaries, and higher nodes are summaries of summaries (clusters of related chunks). At query time, the tree is searched at all levels, retrieving both specific details and broad themes.

The pipeline chunks the document, summarizes each chunk (leaf level), clusters similar summaries and creates higher-level abstractions, then searches across all tree levels for the most relevant context at the right granularity.

## The Solution

**You write the chunking, summarization, clustering, and tree search logic. Conductor handles the hierarchical pipeline, retries, and observability.**

Each stage of the RAPTOR pipeline is an independent worker. document chunking, leaf summary generation, hierarchical clustering/summarization, tree search, and answer generation. Conductor sequences them, retries the LLM summarization if rate-limited, and tracks the full tree construction and search for debugging.

### What You Write: Workers

Five workers build the RAPTOR tree pipeline. chunking documents, generating leaf-level summaries, clustering into hierarchical abstractions, searching across all tree levels, and generating an answer from multi-granularity context.

| Worker | Task | What It Does |
|---|---|---|
| **ChunkDocsWorker** | `rp_chunk_docs` | Worker that chunks a document into smaller segments for RAPTOR tree construction. Takes documentText and returns 6 fi... |
| **ClusterSummariesWorker** | `rp_cluster_summaries` | Worker that builds cluster-level summaries from leaf summaries. Produces 2 cluster summaries at level 1 and a single ... |
| **GenerateWorker** | `rp_generate` | Worker that generates an answer using the question and multi-level context retrieved from the RAPTOR tree. Combines c... |
| **LeafSummariesWorker** | `rp_leaf_summaries` | Worker that creates leaf-level summaries from document chunks. Groups related chunks and produces 4 leaf summaries at... |
| **TreeSearchWorker** | `rp_tree_search` | Worker that searches the RAPTOR tree for context relevant to a question. Traverses the tree top-down (root -> cluster... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
rp_chunk_docs
 │
 ▼
rp_leaf_summaries
 │
 ▼
rp_cluster_summaries
 │
 ▼
rp_tree_search
 │
 ▼
rp_generate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
