# RAG Embedding Selection in Java Using Conductor : Benchmark OpenAI, Cohere, and Local Models in Parallel

## Choosing the Right Embedding Model

Different embedding models produce different quality results for different domains. OpenAI's `text-embedding-3-large` might excel at general knowledge but underperform on technical documentation. Cohere's embeddings might be faster but less accurate for your specific corpus. A local sentence-transformers model might match cloud quality at zero per-query cost. You won't know until you benchmark them side-by-side on your actual data.

The benchmark prepares test queries and documents, embeds them with all three providers in parallel, evaluates each on retrieval quality metrics (recall, MRR) and performance (latency, throughput), and selects the winner.

## The Solution

**You write the embedding calls and evaluation metrics. Conductor handles the parallel benchmarking, retries, and observability.**

Each embedding provider is an independent worker. OpenAI, Cohere, local. Conductor's `FORK_JOIN` runs all three in parallel. An evaluation worker scores each provider's embeddings, and a selection worker picks the best. Every benchmark run records all metrics for all providers, building a dataset for model selection decisions.

### What You Write: Workers

Six workers benchmark embedding providers. preparing test data, running OpenAI, Cohere, and local embeddings in parallel via FORK_JOIN, evaluating each on accuracy and latency, and selecting the best performer for production use.

| Worker | Task | What It Does |
|---|---|---|
| **EmbedCohereWorker** | `es_embed_cohere` | Worker that simulates embedding evaluation using Cohere embed-english-v3.0. Returns pre-computed metrics for the benc... |
| **EmbedLocalWorker** | `es_embed_local` | Worker that simulates embedding evaluation using a local all-MiniLM-L6-v2 model. Returns pre-computed metrics for the... |
| **EmbedOpenaiWorker** | `es_embed_openai` | Worker that simulates embedding evaluation using OpenAI text-embedding-3-large. Returns pre-computed metrics for the ... |
| **EvaluateEmbeddingsWorker** | `es_evaluate_embeddings` | Worker that evaluates embedding metrics from all providers. Computes a composite score for each model and determines ... |
| **PrepareBenchmarkWorker** | `es_prepare_benchmark` | Worker that prepares a benchmark dataset for embedding evaluation. Returns benchmark queries with expected document I... |
| **SelectBestWorker** | `es_select_best` | Worker that selects the best embedding model based on evaluation rankings. Returns the best model, its score, and a r... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
es_prepare_benchmark
 │
 ▼
FORK_JOIN
 ├── es_embed_openai
 ├── es_embed_cohere
 └── es_embed_local
 │
 ▼
JOIN (wait for all branches)
es_evaluate_embeddings
 │
 ▼
es_select_best

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
