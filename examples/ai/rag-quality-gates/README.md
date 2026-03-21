# RAG Quality Gates in Java Using Conductor : Relevance and Faithfulness Checks Before Serving Answers

## Don't Serve Bad Answers

A standard RAG pipeline retrieves and generates without checking quality. If the retrieved documents are irrelevant (the vector store returned noise), the LLM generates a hallucinated answer from bad context. If the answer contradicts the retrieved documents (the LLM went off-script), the user gets incorrect information. Quality gates catch both: a relevance check rejects bad retrievals before generation, and a faithfulness check rejects unfaithful answers before serving.

This creates a pipeline with two conditional branch points. retrieve, check relevance (pass or reject), generate, check faithfulness (pass or reject). Without orchestration, this is nested if/else logic with no visibility into which gate rejected which query.

## The Solution

**You write the relevance and faithfulness check logic. Conductor handles the quality-gated routing, retries, and observability.**

Each step is an independent worker. retrieval, relevance checking, generation, faithfulness checking, rejection. Conductor's `SWITCH` tasks route to rejection when a quality gate fails. Every execution records which gates passed and which rejected, building a dataset for quality monitoring.

### What You Write: Workers

Five workers implement a dual quality gate. retrieving documents, checking relevance (first SWITCH gate), generating an answer, checking faithfulness (second SWITCH gate), and rejecting answers that fail either check.

| Worker | Task | What It Does |
|---|---|---|
| **CheckFaithfulnessWorker** | `qg_check_faithfulness` | Worker that checks the faithfulness of a generated answer against the source documents. Evaluates 3 fixed claims, com... |
| **CheckRelevanceWorker** | `qg_check_relevance` | Worker that checks the relevance of retrieved documents. Computes the average score across all documents and compares... |
| **GenerateWorker** | `qg_generate` | Worker that generates an answer from the question and retrieved documents. Combines the document texts into a synthes... |
| **RejectWorker** | `qg_reject` | Worker that handles rejection when a quality gate fails. Takes a reason and score, and returns rejected=true along wi... |
| **RetrieveWorker** | `qg_retrieve` | Worker that retrieves documents for the given question. Returns 3 fixed documents with id, text, and relevance score.... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
qg_retrieve
 │
 ▼
qg_check_relevance
 │
 ▼
SWITCH (relevance_gate_ref)
 ├── pass: qg_generate -> qg_check_faithfulness -> faithfulness_gate
 ├── fail: qg_reject

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
