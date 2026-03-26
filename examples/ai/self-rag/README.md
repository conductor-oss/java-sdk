# Self-RAG: Retrieve, Grade, Generate, Then Check for Hallucination and Usefulness

A RAG pipeline that checks its own work. After retrieving 4 documents (3 relevant at score >= 0.5, 1 irrelevant at 0.22), it grades them, generates an answer, then runs hallucination detection (score: 0.92) and usefulness grading (score: 0.88). If both scores pass (>= 0.7), the answer is served; otherwise it enters a refinement retry loop.

## Workflow

```
question
    │
    ▼
┌──────────────────┐
│ sr_retrieve      │  4 docs: 3 relevant, 1 irrelevant (0.22)
└────────┬─────────┘
         ▼
┌──────────────────┐
│ sr_grade_docs    │  Filter: keep docs with score >= 0.5
└────────┬─────────┘
         ▼
┌──────────────────┐
│ sr_generate      │  Generate from graded docs
└────────┬─────────┘
         ▼
┌────────────────────────────┐
│ sr_grade_hallucination     │  Score: 0.92, grounded: true
└────────┬───────────────────┘
         ▼
┌──────────────────────────┐
│ sr_grade_usefulness      │  Score: 0.88, verdict: pass/fail
└────────┬─────────────────┘
         ▼
    SWITCH (quality_gate)
    ├── "pass": sr_format_output
    └── default: sr_refine_retry
```

## Workers

**RetrieveWorker** (`sr_retrieve`) -- Returns 4 docs: `d1` (score 0.91, SIMPLE/SYSTEM tasks), `d2` (0.85), `d3` (0.78), and `d4` (0.22, irrelevant).

**GradeDocsWorker** (`sr_grade_docs`) -- Filters documents using `.filter(d -> ((Number) d.get("score")).doubleValue() >= 0.5)`. Logs count of docs passing threshold 0.5.

**GenerateWorker** (`sr_generate`) -- Generates from the graded documents using system prompt.

**GradeHallucinationWorker** (`sr_grade_hallucination`) -- Returns `score: 0.92` and `grounded: true`.

**GradeUsefulnessWorker** (`sr_grade_usefulness`) -- Returns `score: 0.88`. Verdict is `"pass"` if score >= 0.7 AND hallucination score >= 0.7.

**FormatOutputWorker** (`sr_format_output`) -- Formats the final answer for output.

**RefineRetryWorker** (`sr_refine_retry`) -- Handles the refinement loop when quality gate fails.

## Tests

25 tests cover retrieval, score-based grading, generation, hallucination detection, usefulness evaluation, and both SWITCH paths.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
