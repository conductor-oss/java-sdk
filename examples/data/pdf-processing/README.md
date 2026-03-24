# Pdf Processing

A compliance department receives regulatory filings as PDF documents. Each PDF needs text extraction, metadata parsing (author, creation date, page count), content classification by section type, and indexing for full-text search. Scanned-image PDFs need a different extraction path than native-text PDFs.

## Pipeline

```
[pd_extract_text]
     |
     v
[pd_parse_sections]
     |
     v
[pd_analyze_content]
     |
     v
[pd_generate_summary]
```

**Workflow inputs:** `pdfBase64`, `pdfPath`, `options`

## Workers

**AnalyzeContentWorker** (task: `pd_analyze_content`)

Analyzes content of parsed sections — counts words and finds keywords.

- Lowercases strings, rounds with `math.round()`, formats output strings
- Reads `sections`. Writes `wordCount`, `keywords`, `analysis`

**ExtractTextWorker** (task: `pd_extract_text`)

Extracts text from a real PDF document using Apache PDFBox.

- Base64 encodes data
- Reads `pdfPath`, `pdfBase64`, `pdfUrl`. Writes `error`, `text`, `pageCount`, `charCount`

**GenerateSummaryWorker** (task: `pd_generate_summary`)

Generates a summary based on sections and analysis.

- Lowercases strings, uses java streams
- Reads `sections`, `analysis`. Writes `summary`

**ParseSectionsWorker** (task: `pd_parse_sections`)

Parses raw text into sections based on chapter headings.

- Trims whitespace, applies compiled regex
- Reads `rawText`. Writes `sections`, `sectionCount`

---

**34 tests** | Workflow: `pdf_processing` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
