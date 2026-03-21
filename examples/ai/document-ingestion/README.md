# Document Ingestion Pipeline in Java Using Conductor: PDF to Vector Store in Four Steps

Someone dumps 10,000 PDFs into a shared drive and expects the RAG system to answer questions about them by Monday. Your chatbot can't find anything because nobody extracted the text, chunked it for embedding models, generated vectors, or loaded them into the vector store. If the embedding API rate-limits you halfway through, a naive script loses track of what was already processed and starts over from page one. This example builds a four-stage document ingestion pipeline using [Conductor](https://github.com/conductor-oss/conductor): extract, chunk, embed, store, that turns raw PDFs into searchable vectors with per-stage retries and full visibility into which document failed at which step.

## Turning Documents into Searchable Knowledge

Before a RAG system can answer questions, documents must be ingested: raw PDFs need to be parsed into plain text, that text needs to be split into chunks small enough for embedding models (with overlap to preserve context at boundaries), each chunk needs to be embedded into a vector, and those vectors need to be upserted into a collection in your vector store.

Each step depends on the previous one's output. You can't chunk text you haven't extracted, and you can't embed chunks you haven't created. If the embedding API rate-limits you mid-batch or the vector store connection drops during upsert, you need to retry that specific step without re-extracting a 200-page PDF. And when you're ingesting thousands of documents, you need to see exactly which document failed at which stage.

Without orchestration, this becomes a fragile script where a single embedding API timeout means restarting from scratch, with no record of what was already processed.

## The Solution

**You write the extraction, chunking, embedding, and storage logic. Conductor handles the pipeline sequencing, retries, and observability.**

Each stage of the ingestion pipeline is an independent worker. PDF extraction, text chunking, embedding generation, vector storage. Conductor sequences them, passes the output of each stage to the next, retries on transient failures (embedding API timeouts, vector store connection drops), and tracks every document's journey from PDF to stored vectors. You get a example-grade ingestion pipeline without writing retry loops or progress tracking.

### What You Write: Workers

Four workers form the ingestion pipeline. PDF text extraction, word-based chunking with configurable overlap, embedding generation, and vector database upsert, each stage feeding its output to the next.

| Worker | Task | What It Does |
|---|---|---|
| **IngestChunkTextWorker** | `ingest_chunk_text` | Worker 2: Splits extracted text into overlapping chunks. Uses word-based chunking with configurable size and overlap. |
| **IngestEmbedChunksWorker** | `ingest_embed_chunks` | Worker 3: Generates embeddings for each text chunk. Uses fixed (deterministic) embeddings for reproducible tests. |
| **IngestExtractPdfWorker** | `ingest_extract_pdf` | Worker 1: Extracts text from a PDF document. Simulates PDF parsing by returning fixed text about vector databases. |
| **IngestStoreVectorsWorker** | `ingest_store_vectors` | Worker 4: Stores embedding vectors in a vector database collection. Simulates upserting vectors and returns the count |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode, the workflow and worker interfaces stay the same.

### The Workflow

```
ingest_extract_pdf
 │
 ▼
ingest_chunk_text
 │
 ▼
ingest_embed_chunks
 │
 ▼
ingest_store_vectors

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
