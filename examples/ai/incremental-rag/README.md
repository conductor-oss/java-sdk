# Incremental RAG in Java Using Conductor : Sync Only Changed Documents to Your Vector Store

## The Cost of Full Re-Indexing

When documents in your knowledge base change. new articles added, existing ones updated, obsolete ones removed, the vector store needs to reflect those changes. A naive approach re-embeds every document on every sync cycle. For a corpus of 100,000 documents where 50 changed, that's 99,950 wasted embedding API calls and hours of unnecessary processing.

Incremental indexing solves this by comparing document hashes to detect what actually changed, separating new documents (no existing hash) from updated ones (hash mismatch), embedding only those, and upserting the new vectors. After the upsert, a verification step confirms the index is consistent and reports query latency. If the embedding API fails mid-batch, you need to retry from the embedding step without re-running change detection. the changed document list is still valid.

Without orchestration, this becomes a fragile script where an embedding timeout means re-scanning the entire source collection, there's no record of how many documents were synced per run, and a failed upsert leaves the index in an inconsistent state with no way to resume.

## The Solution

**You write the change detection, embedding, and upsert logic. Conductor handles the incremental sync sequencing, retries, and observability.**

Each stage of the incremental sync is an independent worker. change detection, filtering, embedding, upserting, verification. Conductor sequences them and passes change sets between stages. If the embedding API times out, Conductor retries it without re-running change detection. Every sync run is tracked end-to-end, showing exactly how many documents were detected, filtered, embedded, upserted, and verified.

### What You Write: Workers

Five workers handle incremental index maintenance. detecting document changes, filtering to only new or modified content, embedding the new chunks, upserting vectors, and verifying index consistency.

| Worker | Task | What It Does |
|---|---|---|
| **DetectChangesWorker** | `ir_detect_changes` | Worker that detects changed documents in the source collection since the last sync timestamp. Returns changed documen... |
| **EmbedIncrementalWorker** | `ir_embed_incremental` | Worker that generates embeddings for documents that need to be inserted or updated. Uses deterministic vectors rather... |
| **FilterNewDocsWorker** | `ir_filter_new_docs` | Worker that separates changed documents into new (no existing hash) and updated (has existing hash) categories, produ... |
| **UpsertVectorsWorker** | `ir_upsert_vectors` | Worker that upserts embedding vectors into the vector store, counting inserts vs updates based on the action field. |
| **VerifyIndexWorker** | `ir_verify_index` | Worker that verifies the vector index after upserting. Confirms all documents are indexed and reports query latency. |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
ir_detect_changes
 │
 ▼
ir_filter_new_docs
 │
 ▼
ir_embed_incremental
 │
 ▼
ir_upsert_vectors
 │
 ▼
ir_verify_index

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
