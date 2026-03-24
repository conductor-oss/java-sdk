# Benchmarking Embedding Models: OpenAI vs Cohere vs Local in Parallel

Which embedding model gives you the best retrieval quality? This workflow prepares benchmark queries with expected results, runs OpenAI, Cohere, and a local model in parallel via FORK_JOIN, evaluates each with a composite score, and selects the winner.

## Workflow

```
benchmark config
       │
       ▼
┌────────────────────────┐
│ es_prepare_benchmark   │  Build queries with expected doc IDs
└───────────┬────────────┘
            ▼
┌─── FORK_JOIN ─────────────────────────────────────┐
│ ┌──────────────────┐ ┌────────────────┐ ┌────────┐│
│ │es_embed_openai   │ │es_embed_cohere │ │es_embed││
│ │                  │ │                │ │_local  ││
│ └──────────────────┘ └────────────────┘ └────────┘│
└───────────────────────────┬───────────────────────┘
                            ▼
                 ┌──────────────────────────────┐
                 │ es_evaluate_embeddings       │  Composite scoring
                 └──────────────┬───────────────┘
                                ▼
                 ┌──────────────────────────────┐
                 │ es_select_best               │  Pick winner
                 └──────────────────────────────┘
```

## Workers

**PrepareBenchmarkWorker** (`es_prepare_benchmark`) -- Creates benchmark queries like `{id: "q1", text: "How does vector search work?", expectedDocIds: ["doc1", "doc3"]}`.

**EmbedOpenaiWorker** (`es_embed_openai`) -- Embeds via OpenAI and returns performance metrics (NDCG, recall, precision, latency).

**EmbedCohereWorker** (`es_embed_cohere`) -- Embeds via Cohere and returns the same metric structure.

**EmbedLocalWorker** (`es_embed_local`) -- Embeds locally and returns metrics.

**EvaluateEmbeddingsWorker** (`es_evaluate_embeddings`) -- Computes a composite score: `ndcg*0.4 + recall*0.3 + precision*0.2 + (1 - latency/200)*0.1`. Compares all three provider results and produces a ranked evaluation.

**SelectBestWorker** (`es_select_best`) -- Returns the best model, its composite score, and a recommendation string.

## Tests

16 tests cover benchmark preparation, all three embedding providers, evaluation scoring, and model selection.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
