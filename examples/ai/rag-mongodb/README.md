# RAG with MongoDB Atlas Vector Search in Java Using Conductor : Embed, Search, Generate

## RAG on Your Existing MongoDB Data

If your documents already live in MongoDB, Atlas Vector Search lets you run vector similarity queries against the same collection using `$vectorSearch` aggregation stages. no data migration to a separate vector store needed. The RAG pipeline embeds the question, queries MongoDB with the vector, and generates from the results, all against your existing data and indexes.

Each step can fail independently: the embedding API might time out, the MongoDB cluster might be performing an election, or the LLM might be rate-limited.

## The Solution

**You write the embedding and MongoDB Atlas Vector Search query logic. Conductor handles the RAG pipeline, retries, and observability.**

Each stage is an independent worker. question embedding, MongoDB Atlas vector search (specifying database and collection), and answer generation. Conductor sequences them, retries the MongoDB query during replica set elections, and tracks every search with the question, database, collection, retrieved documents, and generated answer.

### What You Write: Workers

Three workers integrate MongoDB Atlas into the RAG pipeline. embedding the query, performing $vectorSearch against a MongoDB Atlas collection, and generating an answer from the matched documents.

| Worker | Task | What It Does |
|---|---|---|
| **MongoEmbedWorker** | `mongo_embed` | Worker that converts a question into a fixed embedding vector. In production this would call an embedding model (e.g.... |
| **MongoGenerateWorker** | `mongo_generate` | Worker that generates an answer from the question and retrieved documents. In production this would call an LLM (e.g.... |
| **MongoVectorSearchWorker** | `mongo_vector_search` | Worker that simulates MongoDB Atlas $vectorSearch aggregation pipeline stage. In production this would run: db.collec... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
mongo_embed
 │
 ▼
mongo_vector_search
 │
 ▼
mongo_generate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
