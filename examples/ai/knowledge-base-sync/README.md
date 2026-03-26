# Crawl-Extract-Update-Index-Verify: Keeping a Knowledge Base Current

A knowledge base with 156 pages of help content goes stale as product features change. This pipeline crawls the source URL, extracts articles from the crawled pages, updates the knowledge base with new and modified articles, rebuilds the search index, and verifies that the index is searchable.

## Workflow

```
sourceUrl, kbId
     │
     ▼
┌──────────────┐
│ kbs_crawl    │  Crawl source, find 156 pages
└──────┬───────┘
       │  pages: ["faq.html", "setup.html", "api.html"], pageCount: 156
       ▼
┌──────────────┐
│ kbs_extract  │  Extract 89 articles from pages
└──────┬───────┘
       │  articles: [{title: "Getting Started"}, ...], articleCount: 89
       ▼
┌──────────────┐
│ kbs_update   │  Apply changes: 34 new, 55 updated
└──────┬───────┘
       │  updatedCount: 89, newArticles: 34, modifiedArticles: 55
       ▼
┌──────────────┐
│ kbs_index    │  Rebuild search index
└──────┬───────┘
       │  indexedCount: 89, indexSizeBytes: 4521984, indexTime: "2.3s"
       ▼
┌──────────────┐
│ kbs_verify   │  Verify search is ready
└──────────────┘
       │
       ▼
  pagesCrawled, articlesUpdated, searchReady
```

## Workers

**CrawlWorker** (`kbs_crawl`) -- Logs the `sourceUrl` and reports finding 156 pages. Returns a sample page list `["faq.html", "setup.html", "api.html"]`.

**ExtractWorker** (`kbs_extract`) -- Reports extracting 89 articles from the crawled pages. Returns sample articles with titles `"Getting Started"` and `"API Reference"`.

**UpdateWorker** (`kbs_update`) -- Logs the `kbId` and reports `34 new, 55 updated, 0 deleted`. Returns `updatedCount: 89`, `newArticles: 34`, `modifiedArticles: 55`.

**IndexWorker** (`kbs_index`) -- Rebuilds the search index. Reports `indexedCount: 89`, `indexSizeBytes: 4521984` (~4.3MB), `indexTime: "2.3s"`.

**VerifyWorker** (`kbs_verify`) -- Confirms search readiness with `searchReady: true` and runs a sample query `{query: "setup guide", results: 5}`.

## Tests

10 tests across 5 test files verify each pipeline stage.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
