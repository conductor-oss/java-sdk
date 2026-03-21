# Summarization Pipeline in Java with Conductor : Extract Sections, Compress, and Generate Summaries

## Turning Long Documents into Actionable Summaries

Long documents. reports, articles, meeting transcripts, legal filings, contain important information buried in pages of text. Reading every word is impractical when you need the key points. Effective summarization requires structure: first identify the document's logical sections (introduction, findings, recommendations), then compress each section to its essential points, and finally generate a cohesive summary that stays within a target length.

This workflow processes a document through three summarization steps. The section extractor identifies logical segments in the text. The compressor reduces each section to its key points while preserving meaning. The summary generator combines the compressed sections into a final summary that respects the `maxLength` constraint. Each step builds on the previous one. you cannot compress sections that have not been identified, and you cannot generate a summary without compressed content.

## The Solution

**You just write the section-extraction, compression, and summary-generation workers. Conductor handles the summarization pipeline and content flow.**

Three workers form the summarization pipeline. section extraction, compression, and summary generation. The extractor identifies logical document sections. The compressor reduces each section to its essential points. The generator produces a final summary within the specified length. Conductor sequences the three steps and passes sections and compressed content between them via JSONPath.

### What You Write: Workers

ExtractSectionsWorker identifies logical document segments, CompressWorker reduces each section to key points, and GenerateSummaryWorker combines the compressed content into a cohesive summary within the target length.

| Worker | Task | What It Does |
|---|---|---|
| **CompressWorker** | `sum_compress` | Reduces each extracted section to its key points, achieving ~80% word-count reduction while preserving meaning. |
| **ExtractSectionsWorker** | `sum_extract_sections` | Identifies and extracts logical sections (intro, findings, recommendations, etc.) from the input document. |
| **GenerateSummaryWorker** | `sum_generate_summary` | Combines compressed sections into a final cohesive summary within the specified maximum length. |

### The Workflow

```
sum_extract_sections
 │
 ▼
sum_compress
 │
 ▼
sum_generate_summary

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
