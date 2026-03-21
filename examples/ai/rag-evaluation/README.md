# RAG Evaluation in Java Using Conductor : Faithfulness, Relevance, and Coherence Scoring in Parallel

## Measuring RAG Quality Systematically

A RAG pipeline can produce answers that are relevant but unfaithful (the answer sounds right but isn't supported by the context), faithful but incoherent (accurate but poorly structured), or coherent but irrelevant (well-written but doesn't address the question). You need all three metrics to assess quality.

Running evaluations sequentially triples the time. Running them in parallel requires thread management and synchronization. And without tracking scores over time, you can't tell whether a prompt change improved faithfulness at the cost of coherence.

## The Solution

**You write the faithfulness, relevance, and coherence scoring logic. Conductor handles the parallel evaluation, retries, and observability.**

The RAG pipeline runs first, producing a question, context, and answer. Then Conductor's `FORK_JOIN` evaluates faithfulness, relevance, and coherence in parallel. An aggregation worker combines the scores into an overall quality rating. Every evaluation is tracked, building a dataset for RAG quality monitoring over time.

### What You Write: Workers

Five workers evaluate RAG quality. running the RAG pipeline, then scoring faithfulness, relevance, and coherence in parallel via FORK_JOIN, and aggregating the three scores into a unified quality report.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateScoresWorker** | `re_aggregate_scores` | Worker that aggregates evaluation scores from faithfulness, relevance, and coherence. Computes an overall average sco... |
| **EvalCoherenceWorker** | `re_eval_coherence` | Worker that evaluates the coherence of a RAG answer. Checks whether the answer is logically structured and well-organ... |
| **EvalFaithfulnessWorker** | `re_eval_faithfulness` | Worker that evaluates the faithfulness of a RAG answer. Checks whether the answer is supported by the retrieved context. |
| **EvalRelevanceWorker** | `re_eval_relevance` | Worker that evaluates the relevance of a RAG answer. Checks whether the answer addresses the original question. |
| **RunRagWorker** | `re_run_rag` | Worker that simulates running a RAG pipeline. Takes a question and returns an answer, context passages, and retrieved... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
re_run_rag
 │
 ▼
FORK_JOIN
 ├── re_eval_faithfulness
 ├── re_eval_relevance
 └── re_eval_coherence
 │
 ▼
JOIN (wait for all branches)
re_aggregate_scores

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
