# File Processing Agent in Java Using Conductor : Detect Type, Extract Content, Analyze, Summarize

File Processing Agent. detect file type, extract content, analyze, and generate summary through a sequential pipeline. ## Different File Types Need Different Processing

A user uploads a file. Is it a PDF contract that needs text extraction and clause identification? A CSV dataset that needs column analysis and statistical summaries? A JSON config file that needs schema validation? An image that needs OCR or object detection? The processing pipeline must adapt to the file type.

File type detection goes beyond MIME types. a `.csv` file might actually be tab-separated, a `.json` file might be malformed, a PDF might be scanned (requiring OCR) or native (requiring text extraction). Each file type needs a different extraction strategy, and the analysis step needs to know what kind of content it's working with (tabular data vs, free text vs: structured config). Without orchestration, this becomes a monolithic processor with nested type-checking and extraction logic that's impossible to extend.

## The Solution

**You write the file detection, extraction, analysis, and summarization logic. Conductor handles the processing pipeline, retries on extraction failures, and per-file-type performance tracking.**

`DetectFileTypeWorker` examines the file metadata (name, size, MIME type) and content signatures to determine the actual file type and processing strategy. `ExtractContentWorker` applies the appropriate extraction method. text extraction for PDFs, parsing for CSVs, deserialization for JSON. `AnalyzeContentWorker` performs type-appropriate analysis. NLP for text content, statistical summaries for tabular data, schema validation for structured data. `GenerateSummaryWorker` produces a structured summary tailored to the content type. Conductor chains these steps and tracks processing time per file type for performance optimization.

### What You Write: Workers

Four workers process uploaded files. Detecting the type, extracting content with the appropriate method, analyzing it, and generating a structured summary.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeContentWorker** | `fp_analyze_content` | Analyzes extracted content. identifies document type, sentiment, topics, named entities, and key findings. |
| **DetectFileTypeWorker** | `fp_detect_file_type` | Detects the file type from the file name extension and returns the file type, extraction method, and category. |
| **ExtractContentWorker** | `fp_extract_content` | Simulates content extraction from a file. Returns structured content (title, sections, pages) and metadata (word coun... |
| **GenerateSummaryWorker** | `fp_generate_summary` | Generates a human-readable summary from the analysis results and key findings. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
fp_detect_file_type
 │
 ▼
fp_extract_content
 │
 ▼
fp_analyze_content
 │
 ▼
fp_generate_summary

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
