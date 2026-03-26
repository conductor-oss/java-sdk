# RAPTOR RAG: Hierarchical Summarization Tree for Multi-Level Retrieval

Flat chunk retrieval misses the big picture. RAPTOR builds a tree: raw chunks at the bottom, leaf summaries grouping related chunks in the middle, cluster summaries at the top. The tree search retrieves context at all 3 levels, giving the LLM both granular details and high-level themes.

## Workflow

```
documentText, question
       │
       ▼
┌──────────────────────┐
│ rp_chunk_docs        │  Split into raw chunks
└───────────┬──────────┘
            ▼
┌──────────────────────┐
│ rp_leaf_summaries    │  Summarize groups of chunks
└───────────┬──────────┘
            ▼
┌──────────────────────┐
│ rp_cluster_summaries │  Summarize groups of leaves
└───────────┬──────────┘
            ▼
┌──────────────────────┐
│ rp_tree_search       │  Search all 3 tree levels
└───────────┬──────────┘
            ▼
┌──────────────────────┐
│ rp_generate          │  Generate from multi-level context
└──────────────────────┘
```

## Workers

**ChunkDocsWorker** (`rp_chunk_docs`) -- Returns raw chunks: `{id: "chunk-1", text: "..."}`, `{id: "chunk-2", text: "..."}`.

**LeafSummariesWorker** (`rp_leaf_summaries`) -- Produces leaf-level summaries with `chunkIds: ["chunk-1", "chunk-2"]` linking back to source chunks.

**ClusterSummariesWorker** (`rp_cluster_summaries`) -- Produces cluster summaries with `leafIds: ["leaf-1", "leaf-2"]` linking to leaves.

**TreeSearchWorker** (`rp_tree_search`) -- Returns context at 3 levels: root (broadest summary), cluster (mid-level), and leaf (most specific), each with relevance scores.

**GenerateWorker** (`rp_generate`) -- Generates from the multi-level context using system prompt.

## Tests

32 tests cover chunk creation, leaf summarization, cluster summarization, tree search, and generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
