# Answering Questions About a Q4 Earnings Report

A user uploads a 24-page Q4 Earnings Report PDF and asks "What was the total revenue?" Instead of reading all 24 pages, the system ingests the document, splits it into 42 semantic chunks, builds a vector index, retrieves the top relevant chunks, and synthesizes a natural-language answer with a confidence score.

## Workflow

```
documentUrl, question
       │
       ▼
┌──────────────┐
│ dqa_ingest   │  Fetch and parse document
└──────┬───────┘
       │  document {title, pages: 24, wordCount: 8500, format: "pdf"}
       ▼
┌──────────────┐
│ dqa_chunk    │  Split into 42 semantic chunks
└──────┬───────┘
       │  chunks[], chunkCount: 42
       ▼
┌──────────────┐
│ dqa_index    │  Create vector index with embeddings
└──────┬───────┘
       │  indexId: "IDX-<base36-timestamp>", dimensions: 1536
       ▼
┌──────────────┐
│ dqa_query    │  Retrieve top relevant chunks
└──────┬───────┘
       │  relevantChunks [{id: 5, text: "Total revenue Q4: $2.4B", score: 0.96}]
       ▼
┌──────────────┐
│ dqa_answer   │  Synthesize answer from chunks
└──────────────┘
       │
       ▼
  chunkCount: 42, answer, confidence: 0.95
```

## Workers

**IngestWorker** (`dqa_ingest`) -- Logs the `documentUrl` from input and returns a document metadata map: `title: "Q4 Earnings Report"`, `pages: 24`, `wordCount: 8500`, `format: "pdf"`.

**ChunkWorker** (`dqa_chunk`) -- Returns a fixed chunk list with one sample entry `{id: 0, text: "Revenue grew 15%"}`, reports `chunkCount: 42` and `strategy: "semantic"`.

**IndexWorker** (`dqa_index`) -- Generates a unique index ID using `"IDX-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase()`. Reports `embeddingModel: "text-embedding-3-small"` and `dimensions: 1536`.

**QueryWorker** (`dqa_query`) -- Returns a list of relevant chunks, with the top result being `{id: 5, text: "Total revenue Q4: $2.4B", score: 0.96}`.

**AnswerWorker** (`dqa_answer`) -- Produces the answer `"Q4 total revenue was $2.4 billion, a 15% YoY increase."` with `confidence: 0.95` and `sourceChunks: [5, 8, 12]` indicating which chunk IDs contributed.

## Tests

10 tests across 5 test files cover each stage of the document QA pipeline.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
