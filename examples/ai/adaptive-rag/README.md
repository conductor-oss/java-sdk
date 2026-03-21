# Adaptive RAG in Java Using Conductor: Classify Query Complexity, Route to Optimal Retrieval Strategy

"What's the capital of France?" gets routed through the full RAG pipeline: embed, search, rerank, generate, burning tokens and adding 3 seconds of latency for an answer the LLM already knows. Meanwhile, "How did the 2008 financial crisis reshape European monetary policy over the following decade?" gets the same single-pass retrieval and produces a shallow, incomplete answer. One-size-fits-all RAG over-engineers simple questions and under-serves complex ones. This example builds an adaptive RAG pipeline using [Conductor](https://github.com/conductor-oss/conductor) that classifies each query by complexity and routes it to the optimal strategy, fast single-pass for factual lookups, multi-hop retrieval with chain-of-thought reasoning for analytical questions, and direct generation for creative queries.

## One RAG Strategy Does Not Fit All Questions

"What is the capital of France?" needs a single vector lookup and a short generation. "How did the 2008 financial crisis affect European monetary policy in the following decade?" needs multi-hop retrieval across multiple documents with a reasoning step to synthesize findings. Sending both through the same RAG pipeline either over-engineers simple questions (wasting tokens and latency) or under-serves complex ones (producing shallow, incomplete answers).

Adaptive RAG classifies each query to determine its complexity. Factual, multi-hop, or analytical, and routes to the retrieval strategy that fits. Simple queries get fast, single-pass retrieval. Multi-hop queries get iterative retrieval with intermediate reasoning. Analytical queries get specialized generation that synthesizes across sources. The classification happens once, and the routing is automatic.

## The Solution

**You write the query classifier and the per-complexity retrieval strategies. Conductor handles the routing, retries, and observability.**

`ClassifyWorker` examines the question and determines its complexity class. Simple, multi-hop, or analytical. A `SWITCH` task routes based on the classification: simple questions go to `SimpleRetrieveWorker` then `SimpleGenerateWorker` for direct retrieval and answer generation. Multi-hop questions go to `MultiHopRetrieveWorker` then `ReasonWorker` for iterative retrieval with intermediate reasoning steps. Analytical questions go to `AnalyticalGenerateWorker` for synthesis-heavy generation. Conductor makes this routing declarative and records which strategy was selected for each query.

### What You Write: Workers

Seven workers span three retrieval strategies: simple lookup, multi-hop reasoning, and creative generation, with a classifier that routes each query to the right path via a SWITCH task.

| Worker | Task | What It Does | Real / Notes |
|---|---|---|---|
| **ClassifyWorker** | `ar_classify` | Examines the question and determines its complexity class (`factual`, `analytical`, or `creative`) with a confidence score, routing it to the optimal retrieval strategy | Requires API key, or swap in a real LLM classifier (Claude, GPT-4) or a fine-tuned BERT model |
| **SimpleRetrieveWorker** | `ar_simple_ret` | Single-pass retrieval for factual queries. Returns basic document chunks from the vector store | Requires API key, or swap in Pinecone, Weaviate, Qdrant, or pgvector |
| **SimpleGenerateWorker** | `ar_simple_gen` | Produces a direct, concise answer from the retrieved documents (factual path) | Requires API key, or swap in Claude Messages API or OpenAI Chat Completions |
| **MultiHopRetrieveWorker** | `ar_mhop_ret` | Iterative multi-hop retrieval for analytical queries. Gathers documents across multiple hops to build a comprehensive evidence base | Requires API key, or swap in iterative vector store queries with query reformulation |
| **ReasoningWorker** | `ar_reason` | Builds a chain-of-thought reasoning trace from the multi-hop retrieved documents, connecting evidence across sources | Requires API key, or swap in Claude or GPT-4 with chain-of-thought prompting |
| **AnalyticalGenerateWorker** | `ar_anal_gen` | Synthesizes a comprehensive analytical answer from the reasoning chain and retrieved documents (analytical path) | Requires API key, or swap in Claude or GPT-4 with synthesis prompting |
| **CreativeGenerateWorker** | `ar_creative_gen` | Produces a free-form creative answer without retrieval (default/creative path) | Requires API key, or swap in any LLM with creative generation settings |

Workers require CONDUCTOR_OPENAI_API_KEY. Retrieval workers use Jaccard similarity over bundled documents.

### The Workflow

```
ar_classify
 │
 ▼
SWITCH (sw_ref)
 ├── factual: ar_simple_ret -> ar_simple_gen
 ├── analytical: ar_mhop_ret -> ar_reason -> ar_anal_gen
 └── default: ar_creative_gen

```

## The Adaptive RAG Pipeline

Not all questions need the same retrieval strategy. Adaptive RAG classifies each query and routes to the optimal pipeline:

1. **Classify** (`ar_classify`): An LLM-based classifier examines the question and determines its complexity: `factual` (single-hop lookup), `analytical` (multi-source synthesis), or `creative` (free-form generation). The classification includes a confidence score.

2. **Route** (SWITCH): Conductor's SWITCH task routes based on the classification:
 - **Factual**: `ar_simple_ret` -> `ar_simple_gen`. Single-pass vector retrieval, then direct answer generation. Fast and cheap.
 - **Analytical**: `ar_mhop_ret` -> `ar_reason` -> `ar_anal_gen`. Multi-hop retrieval across multiple documents, intermediate chain-of-thought reasoning, then synthesis. Thorough but more expensive.
 - **Creative** (default): `ar_creative_gen`. Free-form generation without retrieval. No vector store cost.

This saves tokens and latency on simple questions while giving complex questions the depth they need. Every execution records which strategy was selected, so you can analyze classification accuracy and strategy effectiveness over time.

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
