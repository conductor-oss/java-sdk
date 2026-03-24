# Citation-Verified RAG: Generate Answers with Traceable Source References

Your RAG system generates answers peppered with "[1]" and "[2]" citations -- but those markers may reference documents the LLM never actually retrieved. This pipeline generates answers with inline source markers, extracts every citation from the text, and cross-references each one against the actually-retrieved documents.

## Workflow

```
question
    │
    ▼
┌──────────────────────┐
│ cr_retrieve_docs     │  Retrieve 4 documents with relevance scores
└───────────┬──────────┘
            ▼
┌──────────────────────┐
│ cr_generate_cited    │  Generate answer with [1], [2] markers
└───────────┬──────────┘
            ▼
┌──────────────────────┐
│ cr_extract_citations │  Parse all citation markers from text
└───────────┬──────────┘
            ▼
┌──────────────────────┐
│ cr_verify_citations  │  Cross-reference markers against docs
└──────────────────────┘
```

## Workers

**RetrieveDocsWorker** (`cr_retrieve_docs`) -- Returns 4 documents with `id`, `title`, `page`, `text`, and `relevance` score.

**GenerateCitedWorker** (`cr_generate_cited`) -- Produces an answer with inline citations. Returns a `citations` list where each entry has `marker` (e.g., `"[1]"`), `docId`, `page`, `confidence` (e.g., 0.96), and a `quote` from the source.

**ExtractCitationsWorker** (`cr_extract_citations`) -- Parses the generated answer text, checking `answer.contains(marker)` for each citation marker to confirm it appears in the output.

**VerifyCitationsWorker** (`cr_verify_citations`) -- Cross-references each citation's `docId` against the retrieved document list using `docIds.contains(docId)`. Returns `verified: true/false` for each citation.

## Tests

22 tests cover document retrieval, citation generation, extraction, and verification logic.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
