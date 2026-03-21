# RAG with pgvector in Java Using Conductor : Vector Search in PostgreSQL

## RAG Without a Separate Vector Database

pgvector turns any PostgreSQL table into a vector store. You add a `vector(1536)` column to your existing table, create an ivfflat or HNSW index, and query with `ORDER BY embedding <=> $1 LIMIT 10`. Your documents, metadata, and vectors live in the same database. no data synchronization between a document store and a vector store.

The RAG pipeline embeds the question, runs the pgvector similarity query, and generates from the results. Each step can fail independently, and you want the Postgres query retried during temporary connection issues without re-embedding.

## The Solution

**You write the embedding and pgvector SQL query logic. Conductor handles the RAG pipeline, retries, and observability.**

Each stage is an independent worker. question embedding, pgvector SQL query (specifying the table), and answer generation. Conductor sequences them, retries the database query during connection blips, and tracks every search with the question, retrieved rows, and generated answer.

### What You Write: Workers

Three workers integrate pgvector into the RAG pipeline. embedding the query, running SQL-based vector similarity queries against a PostgreSQL table with the pgvector extension, and generating an answer from the results.

| Worker | Task | What It Does |
|---|---|---|
| **PgvecEmbedWorker** | `pgvec_embed` | Worker that encodes a question into a fixed embedding vector for pgvector. In production this would call an embedding... |
| **PgvecGenerateWorker** | `pgvec_generate` | Worker that generates an answer from a question and retrieved pgvector rows. In production this would call an LLM (e.... |
| **PgvecQueryWorker** | `pgvec_query` | Worker that builds a pgvector SQL query and returns demo result rows. Takes embedding, table name, limit, and di... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
pgvec_embed
 │
 ▼
pgvec_query
 │
 ▼
pgvec_generate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
