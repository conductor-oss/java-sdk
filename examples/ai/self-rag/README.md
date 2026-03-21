# Self-RAG in Java Using Conductor : Self-Reflective Retrieval with Hallucination and Usefulness Grading

## A RAG Pipeline That Checks Its Own Work

Standard RAG generates and serves. no self-reflection. Self-RAG adds three grading steps: after retrieval, grade documents for relevance. After generation, grade the answer for hallucination (does it go beyond what the context supports?) and usefulness (does it actually answer the question?). If the answer passes both checks, format and return it. If it fails, route to a refinement step that retries with adjusted parameters.

This creates a conditional pipeline with a quality gate after generation: retrieve, grade docs, generate, grade hallucination, grade usefulness, then branch. format output on success, refine and retry on failure.

## The Solution

**You write the retrieval grading, hallucination detection, and usefulness scoring logic. Conductor handles the self-reflective routing, retries, and observability.**

Each grading step is an independent worker. document grading, hallucination grading, usefulness grading. Conductor's `SWITCH` task routes to either the output formatter or the refinement step based on quality scores. Every execution records all grading scores, making it easy to see which questions trigger refinement and why.

### What You Write: Workers

Seven workers implement the self-reflective pipeline. retrieval, document grading, generation, hallucination grading, usefulness grading, output formatting on pass, and query refinement on fail, with a SWITCH gate after the quality checks.

| Worker | Task | What It Does |
|---|---|---|
| **FormatOutputWorker** | `sr_format_output` | Formats the final output when quality gate passes. Returns {answer, sourceCount}. |
| **GenerateWorker** | `sr_generate` | Generates an answer from relevant documents. Returns a fixed answer about Conductor task types. |
| **GradeDocsWorker** | `sr_grade_docs` | Grades retrieved documents for relevance. Filters documents with score >= 0.5. Returns {relevantDocs, filteredCount}. |
| **GradeHallucinationWorker** | `sr_grade_hallucination` | Grades the generated answer for hallucination against source docs. Returns {score: 0.92, grounded: true}. |
| **GradeUsefulnessWorker** | `sr_grade_usefulness` | Grades the generated answer for usefulness: score = 0.88. If score >= 0.7 AND halScore >= 0.7, verdict = "pass". Retu... |
| **RefineRetryWorker** | `sr_refine_retry` | Refines the query when quality gate fails. Returns {refinedQuery}. |
| **RetrieveWorker** | `sr_retrieve` | Retrieves documents relevant to a question. Returns 4 docs: 3 relevant (score >= 0.5) and 1 irrelevant (0.22). |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
sr_retrieve
 │
 ▼
sr_grade_docs
 │
 ▼
sr_generate
 │
 ▼
sr_grade_hallucination
 │
 ▼
sr_grade_usefulness
 │
 ▼
SWITCH (switch_ref)
 ├── pass: sr_format_output
 └── default: sr_refine_retry

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
