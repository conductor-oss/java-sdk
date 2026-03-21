# Semi-Structured RAG in Java Using Conductor : Parallel Structured and Unstructured Search with Classification

## When Answers Live in Both Tables and Documents

Enterprise knowledge spans structured data (databases, spreadsheets, APIs) and unstructured data (documents, emails, wikis). A question might need revenue figures from a database and context from an analyst report. Querying only one source gives an incomplete answer.

Semi-structured RAG classifies the question first (does it need structured data, unstructured data, or both?), then searches the appropriate sources in parallel. A merge step combines SQL results with document passages into a unified context for generation.

## The Solution

**You write the data classification and the structured/unstructured search logic. Conductor handles the parallel retrieval, merging, and observability.**

A classifier determines which sources to search. Conductor's `FORK_JOIN` runs structured and unstructured searches in parallel. A merge worker combines the results, and a generation worker produces the answer from the unified context. If the database query is slow, Conductor retries it without re-running the document search.

### What You Write: Workers

Five workers handle dual-source retrieval. classifying the question's data needs, searching structured and unstructured sources in parallel via FORK_JOIN, merging both result types, and generating a unified answer.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyDataWorker** | `ss_classify_data` | Worker that classifies input data into structured fields and unstructured text chunks. Returns structuredFields (fiel... |
| **GenerateWorker** | `ss_generate` | Worker that generates a final answer from the question and merged context. Simulates LLM generation by producing a de... |
| **MergeResultsWorker** | `ss_merge_results` | Worker that merges structured and unstructured search results into a unified context string. Formats structured resul... |
| **SearchStructuredWorker** | `ss_search_structured` | Worker that searches structured data sources based on the classified structured fields. Returns results with field, v... |
| **SearchUnstructuredWorker** | `ss_search_unstructured` | Worker that searches unstructured text chunks for relevant passages. Returns results with chunkId, relevance score, a... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
ss_classify_data
 │
 ▼
FORK_JOIN
 ├── ss_search_structured
 └── ss_search_unstructured
 │
 ▼
JOIN (wait for all branches)
ss_merge_results
 │
 ▼
ss_generate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
