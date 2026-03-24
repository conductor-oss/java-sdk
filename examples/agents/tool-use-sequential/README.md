# Tool Use Sequential: Search, Read, Extract, Summarize

A researcher needs an authoritative summary of a technical topic. A single web search returns a list of URLs, but URLs are not answers. The system needs to search, pick the best result, fetch the full page content, extract structured facts from the prose, and distill everything into a coherent summary with a confidence score.

This workflow chains four tools in strict sequence, where each tool's output becomes the next tool's input: search the web, read the top page, extract structured data, and produce a final summary.

## Pipeline Architecture

```
query, maxResults
       |
       v
ts_search_web            (results list, topResult, totalFound=2450)
       |
       v
ts_read_page             (content with sections, statusCode=200, loadTimeMs=320)
       |
       v
ts_extract_data          (facts, keyFeatures, useCases, relevanceScore=0.92)
       |
       v
ts_summarize             (summary, confidence=0.94, wordCount)
```

## Worker: SearchWeb (`ts_search_web`)

Returns three search results as `List<Map<String, String>>` with `url`, `title`, and `snippet` fields. Results point to Orkes and Netflix Conductor documentation. Exposes `topResult` separately for easy downstream wiring. Reports `totalFound: 2450`.

## Worker: ReadPage (`ts_read_page`)

Reads the page at the given URL and returns a `content` map containing `title`, `url`, `wordCount: 1850`, and a `sections` list of four maps, each with `heading` and `text` fields covering "What is Conductor?", "Key Features", "Architecture", and "Use Cases". Reports `statusCode: 200` and `loadTimeMs: 320`.

## Worker: ExtractData (`ts_extract_data`)

Extracts structured data from page content. Returns a `data` map with `facts` (5 items about Conductor's origin, distributed execution, and fault tolerance), `keyFeatures` (5 items: task queuing, workflow versioning, error handling, visual designer, sync/async execution), `useCases` (5 items: media processing, microservice orchestration, data pipelines, CI/CD, AI/ML), `origin: "Netflix"`, and `type: "open-source"`. Reports `relevanceScore: 0.92`.

## Worker: Summarize (`ts_summarize`)

Produces a coherent natural-language summary from the extracted data. Computes `wordCount` via `split("\\s+").length` on the summary text. Returns `confidence: 0.94` and the `sourceUrl` for attribution.

## Tests

4 tests cover web search, page reading, data extraction, and summarization.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
