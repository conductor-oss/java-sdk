# File Processing: Detect Type, Extract Content, Analyze, Summarize

A file arrives with its name and MIME type. The agent detects the file type using an `EXTENSION_MAP`, extracts content (including sections), analyzes topics (`["revenue", "growth", "enterprise", "AI", "margins"]`), and generates a summary (counting words via `summary.trim().split("\\s+")`).

## Workflow

```
fileName, fileSize, mimeType
  -> fp_detect_file_type -> fp_extract_content -> fp_analyze_content -> fp_generate_summary
```

## Workers

**DetectFileTypeWorker** (`fp_detect_file_type`) -- Uses a static `EXTENSION_MAP` to map extensions to file types.

**ExtractContentWorker** (`fp_extract_content`) -- Returns content with sections list.

**AnalyzeContentWorker** (`fp_analyze_content`) -- Returns `topics: ["revenue", "growth", "enterprise", "AI", "margins"]`.

**GenerateSummaryWorker** (`fp_generate_summary`) -- Counts words via `split("\\s+")`.

## Tests

36 tests cover file type detection, content extraction, topic analysis, and summarization.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
