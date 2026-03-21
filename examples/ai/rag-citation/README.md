# RAG Citation in Java Using Conductor: Generate Answers with Source Citations and Verification

Your RAG system gives a great answer, but when the VP asks "where did you get that number?" you can't point to a source. Worse, the LLM peppered the response with "[1]" and "[2]" citations that reference documents it never actually retrieved. fabricated footnotes that look authoritative but lead nowhere. This example builds a citation-verified RAG pipeline using [Conductor](https://github.com/conductor-oss/conductor), generating answers with inline source markers, extracting every citation from the text, and cross-referencing each one against the documents that were actually retrieved, so every claim traces back to a real source or gets flagged.

## Trust But Verify: Citations in RAG Answers

LLMs can claim to cite sources while actually hallucinating references. A standard RAG pipeline provides documents as context, but the generated answer might reference "[3]" when only two documents were retrieved, or cite a document that doesn't actually support the claim. Users who trust these fake citations end up citing nonexistent sources in their own work.

Citation-verified RAG solves this in four steps: retrieve source documents, generate an answer with the LLM instructed to use inline citations, extract all citation references from the generated text, and verify each citation maps to an actual retrieved document. Invalid citations are flagged, and the verification status is included in the output.

## The Solution

**You write the citation generation and verification logic. Conductor handles the pipeline sequencing, retries, and observability.**

Each stage is an independent worker. Document retrieval, cited answer generation, citation extraction (parsing "[1]", "[2]" references from the text), and citation verification (checking each reference against the retrieved document list). Conductor sequences them and tracks every execution with the question, retrieved documents, generated answer, extracted citations, and verification results.

### What You Write: Workers

Four workers form the citation lifecycle. Document retrieval, cited answer generation, citation extraction from the answer text, and cross-reference verification against the source documents.

| Worker | Task | What It Does |
|---|---|---|
| **RetrieveDocsWorker** | `cr_retrieve_docs` | Retrieves 4 source documents with id, title, page number, text, and relevance score (0.83-0.95) from the knowledge base |
| **GenerateCitedWorker** | `cr_generate_cited` | Generates an answer with inline citation markers `[1]` `[2]` `[3]` `[4]` and produces a structured citations array mapping each marker to its source document id, page, confidence score, and the specific claim being cited |
| **ExtractCitationsWorker** | `cr_extract_citations` | Parses the generated answer text for citation markers, checks whether each marker actually appears in the answer, and reports the count of citations found vs, claimed |
| **VerifyCitationsWorker** | `cr_verify_citations` | Cross-references each citation's `docId` against the retrieved document set, flagging any citation that references a non-existent document; returns per-citation verification status and an `allVerified` boolean |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode, the workflow and worker interfaces stay the same.

### The Workflow

```
cr_retrieve_docs
 │
 ▼
cr_generate_cited
 │
 ▼
cr_extract_citations
 │
 ▼
cr_verify_citations

```

## The Citation-Verified RAG Pipeline

Standard RAG generates answers from retrieved context, but there is no guarantee the LLM actually cites its sources correctly. This pipeline adds citation tracking and verification in four stages:

1. **Retrieve** (`cr_retrieve_docs`): Fetch source documents from the knowledge base, each with an id, title, page number, and relevance score.

2. **Generate with Citations** (`cr_generate_cited`): The LLM generates an answer using the retrieved documents as context, with explicit instructions to use inline citation markers (`[1]`, `[2]`, etc.). The output includes both the answer text and a structured citations array mapping each marker to its source document, page, confidence score, and the specific claim being cited.

3. **Extract Citations** (`cr_extract_citations`): Parse the generated answer text for citation markers and verify each one actually appears in the answer. This catches cases where the citations array claims a reference that does not appear in the answer text.

4. **Verify Citations** (`cr_verify_citations`): Cross-reference each citation's `docId` against the retrieved document set. This catches hallucinated citations, where the LLM references a document that was never retrieved (e.g., citing `[5]` when only 4 documents were provided).

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
