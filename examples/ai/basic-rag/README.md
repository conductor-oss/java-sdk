# Basic RAG in Java Using Conductor: Embed Query, Search Vectors, Generate Answer

A user asks your chatbot "What's our refund policy?" and it confidently invents a policy that doesn't exist. because the LLM has zero access to your actual documents. Without retrieval, every answer is a plausible-sounding fabrication. This example builds a three-stage RAG pipeline using [Conductor](https://github.com/conductor-oss/conductor), embed the question, search a vector store for real document chunks, and generate an answer grounded in what was actually retrieved, so the LLM can only cite facts you control.

## LLMs Hallucinate Without Grounding in Your Data

You ask an LLM about your company's refund policy and it invents a plausible-sounding but completely wrong answer. RAG fixes this by retrieving the actual policy document and providing it as context to the LLM, so the answer is grounded in your real data. But RAG has three distinct stages. Embedding the question, searching the vector store, and generating with context, and each can fail independently: the embedding API might timeout, the vector search might return irrelevant results, or the LLM generation might fail due to rate limiting.

Building RAG as a single function means a retry in the embedding step re-runs the entire pipeline, a vector search failure crashes the generation, and there's no visibility into which stage produced poor results. Was the answer bad because the retrieval missed the right document, or because the LLM ignored the context?

## The Solution

**You write the embedding, retrieval, and generation logic. Conductor handles sequencing, durability, and observability.**

`EmbedQueryWorker` converts the user's question into a vector embedding using an embedding model. `SearchVectorsWorker` queries the vector database with the embedding to retrieve the top-k most relevant document chunks. `GenerateAnswerWorker` sends the original question plus the retrieved context to the LLM to produce a grounded answer. Conductor runs these three stages in sequence, records the embedding, retrieved chunks, and generated answer, and lets you tune retry behavior per task. In this example the task defs use `retryCount=0` so live provider failures are surfaced immediately while you validate the pipeline.

### What You Write: Workers

Three workers cover the full RAG pipeline: embedding the query, searching the vector store, and generating an answer, each independently testable and deployable.

| Worker | Task | What It Does |
|---|---|---|
| **EmbedQueryWorker** | `brag_embed_query` | Converts the user's question into a vector embedding using OpenAI (`OPENAI_EMBED_MODEL`, default `text-embedding-3-small`), returning the embedding array, model name, and dimensions | **Real** when `CONDUCTOR_OPENAI_API_KEY` is set; demo (fixed 8-dim vector) otherwise |
| **SearchVectorsWorker** | `brag_search_vectors` | Queries a vector database with the embedding to retrieve the top-k most relevant document chunks, each with id, text, and similarity score (0.85-0.94) | **Always demo**.; no real vector DB. For real vector search, see the `rag-pinecone`, `rag-chromadb`, and `rag-pgvector` examples |
| **GenerateAnswerWorker** | `brag_generate_answer` | Sends the original question plus retrieved context to OpenAI (`OPENAI_CHAT_MODEL`, default `gpt-4o-mini`), producing a grounded answer; returns the answer text and token count | **Real** when `CONDUCTOR_OPENAI_API_KEY` is set; demo (fixed answer) otherwise |

**Important:** Vector search is demo-only in this example, even in live mode. `CONDUCTOR_OPENAI_API_KEY` only turns on real embedding and generation calls. For real vector search, see the `rag-pinecone`, `rag-chromadb`, and `rag-pgvector` examples. Without the key, all three workers produce deterministic demo output so the workflow runs end-to-end without any external dependencies.

### The Workflow

```
brag_embed_query
 │
 ▼
brag_search_vectors
 │
 ▼
brag_generate_answer

```

## The RAG Pipeline

This example implements the foundational Retrieval-Augmented Generation pattern in three stages:

1. **Embed** (`brag_embed_query`): The user's question is converted into a dense vector representation using an embedding model. This vector captures the semantic meaning of the question, enabling similarity-based search rather than keyword matching.

2. **Search** (`brag_search_vectors`): The query embedding is compared against pre-indexed document embeddings in a vector database. The top-k most similar document chunks are returned, ranked by cosine similarity score. Each chunk includes the source text and a relevance score.

3. **Generate** (`brag_generate_answer`): The original question and the retrieved document chunks are sent to an LLM. The model generates an answer grounded in the provided context, reducing hallucination by anchoring responses in actual source material.

Conductor orchestrates these three stages as independent workers. If the embedding API times out, Conductor retries just that step. If the vector search returns but the LLM call fails, the retrieved documents are preserved and only generation is retried. Every execution records the embedding, retrieved chunks, and generated answer, so you can diagnose whether a bad answer came from poor retrieval or poor generation.

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
