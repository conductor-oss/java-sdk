# Evaluating RAG Quality: Faithfulness, Relevance, and Coherence in Parallel

How good are your RAG answers? This pipeline runs a RAG query, then evaluates the result on three dimensions in parallel: faithfulness (0.92), relevance (0.88), and coherence (0.95). The scores are aggregated into an overall verdict (PASS/MARGINAL/FAIL).

## Workflow

```
question, context
       │
       ▼
┌───────────────┐
│ re_run_rag    │  Execute RAG pipeline
└──────┬────────┘
       ▼
┌─── FORK_JOIN ──────────────────────────────────────┐
│ ┌───────────────────┐ ┌────────────┐ ┌────────────┐│
│ │re_eval_faithfulness│ │re_eval_    │ │re_eval_    ││
│ │(score: 0.92)       │ │relevance   │ │coherence   ││
│ │                    │ │(0.88)      │ │(0.95)      ││
│ └───────────────────┘ └────────────┘ └────────────┘│
└──────────────────────────┬─────────────────────────┘
                           ▼
                ┌─────────────────────────┐
                │ re_aggregate_scores     │  Overall verdict
                └─────────────────────────┘
```

## Workers

**RunRagWorker** (`re_run_rag`) -- Executes the RAG pipeline with context documents. Calls `gpt-4o-mini` when API key is set.

**EvalFaithfulnessWorker** (`re_eval_faithfulness`) -- Returns `score: 0.92` with a reason string.

**EvalRelevanceWorker** (`re_eval_relevance`) -- Returns `score: 0.88` with a reason string.

**EvalCoherenceWorker** (`re_eval_coherence`) -- Returns `score: 0.95` with a reason string.

**AggregateScoresWorker** (`re_aggregate_scores`) -- Computes an overall average, determines verdict (PASS/MARGINAL/FAIL based on thresholds), and returns the breakdown.

## Tests

19 tests cover RAG execution and all three evaluation dimensions plus aggregation logic.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
