# Syncing Only Changed Documents to the Vector Store

When 50 out of 100,000 documents change, re-embedding the entire corpus wastes 99,950 API calls. This workflow detects changed documents by comparing content hashes, separates them into inserts vs updates, embeds only the delta, upserts into the vector store, and verifies the index.

## Workflow

```
sourceCollection, lastSyncTimestamp
       │
       ▼
┌─────────────────────┐
│ ir_detect_changes   │  Find changed doc IDs + existing hashes
└──────────┬──────────┘
           │  changedDocIds [4], existingHashes
           ▼
┌─────────────────────┐
│ ir_filter_new_docs  │  Separate inserts from updates
└──────────┬──────────┘
           │  docsToEmbed [{action: "insert"/"update"}]
           ▼
┌─────────────────────┐
│ ir_embed_incremental│  Generate embeddings for delta
└──────────┬──────────┘
           │  embeddings, docIds
           ▼
┌─────────────────────┐
│ ir_upsert_vectors   │  Upsert into vector store
└──────────┬──────────┘
           │  upsertedCount, inserted, updated
           ▼
┌─────────────────────┐
│ ir_verify_index     │  Confirm index integrity
└─────────────────────┘
           │
           ▼
  totalChanged, newCount, updatedCount, verified
```

## Workers

**DetectChangesWorker** (`ir_detect_changes`) -- Returns 4 changed doc IDs: `["doc-101", "doc-205", "doc-307", "doc-412"]`. Provides `existingHashes` as a `HashMap` (supports null values for new docs): `doc-101` has hash `"a1b2c3"`, `doc-307` has `"d4e5f6"`, while `doc-205` and `doc-412` have null (new documents).

**FilterNewDocsWorker** (`ir_filter_new_docs`) -- Iterates `changedDocIds`, checks each against `existingHashes`. Null hash means `action: "insert"` with text `"Content of " + docId`; non-null hash means `action: "update"` with `"Updated content of " + docId`. Counts inserts and updates separately.

**EmbedIncrementalWorker** (`ir_embed_incremental`) -- When `CONDUCTOR_OPENAI_API_KEY` is set, calls OpenAI Embeddings with `text-embedding-3-small` for each doc. In fallback mode, assigns a fixed 5-dimensional vector `[0.12, -0.34, 0.56, 0.78, -0.91]` stored as `DETERMINISTIC_VECTOR`. Preserves the `action` field ("insert"/"update") on each embedding entry. On API failure for a single doc, falls back to deterministic for that doc and continues.

**UpsertVectorsWorker** (`ir_upsert_vectors`) -- Iterates embeddings, counting entries with `action: "insert"` vs `"update"`. Reports `upsertedCount`, `inserted`, `updated`, and the full `docIds` list.

**VerifyIndexWorker** (`ir_verify_index`) -- Confirms all upserted vectors are indexed. Reports `verified: true`, `vectorCount` (matching `upsertedCount`), and `queryLatencyMs: 12`.

## Tests

17 tests cover change detection, filtering logic, embedding with API fallback, upsert counting, and index verification.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
