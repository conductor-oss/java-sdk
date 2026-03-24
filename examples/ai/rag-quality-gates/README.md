# Two Quality Gates: Relevance and Faithfulness Checks Before Serving Answers

RAG pipelines that blindly serve generated answers risk delivering irrelevant or hallucinated content. This workflow adds two sequential quality gates: first checking retrieval relevance (threshold 0.7), then checking answer faithfulness (threshold 0.8). Each gate uses a SWITCH to either continue or reject with the reason and score.

## Workflow

```
question
    │
    ▼
┌──────────────────┐
│ qg_retrieve      │  Retrieve 3 docs (scores: 0.92, ...)
└────────┬─────────┘
         ▼
┌──────────────────┐
│ qg_check_relevance│  Avg score vs threshold 0.7
└────────┬─────────┘
         ▼
    SWITCH (relevance_gate)
    ├── "pass":
    │     ┌──────────────┐
    │     │ qg_generate  │
    │     └──────┬───────┘
    │            ▼
    │     ┌────────────────────────┐
    │     │ qg_check_faithfulness  │  Score vs threshold 0.8
    │     └──────────┬─────────────┘
    │                ▼
    │          SWITCH (faithfulness_gate)
    │          ├── "pass": (serve answer)
    │          └── "fail": qg_reject
    └── "fail":
          qg_reject
```

## Workers

**RetrieveWorker** (`qg_retrieve`) -- Returns 3 docs: `{id: "doc-1", text: "Conductor orchestrates microservices...", score: 0.92}` and two more.

**CheckRelevanceWorker** (`qg_check_relevance`) -- Computes average score across documents and compares against threshold 0.7. Returns `relevanceScore`, `threshold`, and `decision` (pass/fail).

**GenerateWorker** (`qg_generate`) -- Calls `gpt-4o-mini` when API key is set.

**CheckFaithfulnessWorker** (`qg_check_faithfulness`) -- Extracts claims from the answer, scores faithfulness, compares against threshold 0.8. Returns `faithfulnessScore`, `claims` list, `threshold`, and `decision`.

**RejectWorker** (`qg_reject`) -- Takes a `reason` and `score`, returns `rejected: true` with details.

## Tests

28 tests cover retrieval, both quality gate thresholds, generation, and rejection paths.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
