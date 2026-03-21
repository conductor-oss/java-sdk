# Document QA in Java with Conductor : Ingest, Chunk, Index, and Answer Questions About Documents

## Asking Questions Instead of Reading Entire Documents

Users should not have to read a 50-page document to find one answer. Document QA systems let users ask natural language questions and get answers with confidence scores. But building this requires a pipeline: fetch the document, split it into chunks small enough for semantic search, index those chunks, find the most relevant ones for the question, and synthesize an answer from the retrieved context.

This workflow handles the full pipeline. The ingester fetches the document from the provided URL. The chunker splits the document into manageable segments. The indexer stores the chunks in a searchable index. The query worker searches the index using the user's question and retrieves the most relevant chunks. The answer worker synthesizes a natural language answer from those chunks along with a confidence score.

## The Solution

**You just write the document-ingestion, chunking, indexing, querying, and answer-generation workers. Conductor handles the five-step RAG pipeline.**

Five workers form the QA pipeline. document ingestion, chunking, indexing, querying, and answering. The ingester downloads and parses the document. The chunker splits it into segments suitable for semantic search. The indexer creates a searchable index. The query worker finds the chunks most relevant to the question. The answer worker generates a response from those chunks with a confidence score. Conductor sequences all five steps and passes the document content, chunks, index ID, and relevant chunks between them.

### What You Write: Workers

IngestWorker fetches the document, ChunkWorker splits it for search, IndexWorker creates the vector index, QueryWorker retrieves relevant chunks, and AnswerWorker synthesizes a response with a confidence score.

| Worker | Task | What It Does |
|---|---|---|
| **AnswerWorker** | `dqa_answer` | Synthesizes a natural-language answer from the retrieved chunks, with a confidence score. |
| **ChunkWorker** | `dqa_chunk` | Splits the ingested document into semantically meaningful chunks for search indexing. |
| **IndexWorker** | `dqa_index` | Creates a vector index from the document chunks using an embedding model. |
| **IngestWorker** | `dqa_ingest` | Fetches and parses the document from the provided URL (PDF, HTML, etc.). |
| **QueryWorker** | `dqa_query` | Searches the index with the user's question and retrieves the top relevant chunks. |

### The Workflow

```
dqa_ingest
 │
 ▼
dqa_chunk
 │
 ▼
dqa_index
 │
 ▼
dqa_query
 │
 ▼
dqa_answer

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
