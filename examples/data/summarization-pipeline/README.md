# Summarization Pipeline

A news aggregation service collects articles from hundreds of RSS feeds. Each article needs text extraction, key-phrase identification, and summary generation so readers get a 2-3 sentence digest instead of clicking through to every source.

## Pipeline

```
[sum_extract_sections]
     |
     v
[sum_compress]
     |
     v
[sum_generate_summary]
```

**Workflow inputs:** `document`, `maxLength`

## Workers

**ExtractSectionsWorker** (task: `sum_extract_sections`)

Splits a document into titled sections with per-section word counts. Produces four sections -- "Introduction" (450 words), "Methodology" (820 words), "Results" (600 words), and "Conclusion" (280 words) -- totaling 2,150 words. Each section carries a `key` sentence used downstream for compression.

- Writes `sections`, `sectionCount`, `totalWords`

**CompressWorker** (task: `sum_compress`)

Compresses extracted sections by retaining only `title` and `keySentence` from each section map, discarding body text. Reports an 80% reduction (2,150 words down to 420) via `compressionRatio` = `"80%"` and `compressedWords` = 420. Uses Java streams to map each section to its compressed form.

- Reads `sections`. Writes `compressed`, `compressionRatio`, `compressedWords`

**GenerateSummaryWorker** (task: `sum_generate_summary`)

Generates a final natural-language summary from the compressed sections. Outputs the summary text, its `wordCount`, and the model identifier `"bart-large-cnn"`. The summary captures the core finding: 95.2% accuracy on the benchmark dataset using transformer architecture with attention mechanisms.

- Writes `summary`, `wordCount`, `model`

---

**6 tests** | Workflow: `sum_summarization_pipeline` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
