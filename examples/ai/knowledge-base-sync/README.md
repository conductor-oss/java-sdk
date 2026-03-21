# Knowledge Base Sync in Java with Conductor : Crawl, Extract, Update, Index, and Verify

## Keeping Help Content Up to Date

Knowledge bases go stale. Product features change, documentation gets updated, and new articles are published; but the knowledge base that customers and support agents search still shows last month's content. Syncing manually is tedious and error-prone. A reliable sync pipeline needs to crawl the source for changes, extract the content, update the knowledge base, rebuild the search index, and verify everything landed correctly.

This workflow automates the full sync cycle. The crawler scans the source URL and identifies new or changed pages. The extractor pulls structured content (title, body, categories) from the crawled pages. The updater writes the extracted content to the knowledge base. The indexer rebuilds the search index to include the new content. The verifier checks that the updated articles are accessible and the index returns correct results. Each step depends on the previous one. you cannot extract from uncrawled pages or index unupdated content.

## The Solution

**You just write the crawling, extraction, updating, indexing, and verification workers. Conductor handles the five-step sync pipeline and data flow.**

Five workers handle the sync lifecycle. crawling, extraction, updating, indexing, and verification. The crawler discovers changed content at the source. The extractor parses pages into structured articles. The updater writes to the knowledge base. The indexer rebuilds search. The verifier confirms the sync is correct. Conductor sequences all five steps and passes crawled pages, extracted content, and update status between them via JSONPath.

### What You Write: Workers

CrawlWorker discovers changed pages, ExtractWorker parses article content, UpdateWorker writes to the KB, IndexWorker rebuilds search, and VerifyWorker confirms the sync is correct.

| Worker | Task | What It Does |
|---|---|---|
| **CrawlWorker** | `kbs_crawl` | Scans the source URL and discovers new or changed pages to process. |
| **ExtractWorker** | `kbs_extract` | Pulls structured article content (title, body, categories) from crawled pages. |
| **IndexWorker** | `kbs_index` | Rebuilds the search index to include newly updated articles. |
| **UpdateWorker** | `kbs_update` | Writes extracted articles to the knowledge base, tracking new, updated, and deleted counts. |
| **VerifyWorker** | `kbs_verify` | Confirms that all indexed articles are searchable and the sync completed correctly. |

### The Workflow

```
kbs_crawl
 │
 ▼
kbs_extract
 │
 ▼
kbs_update
 │
 ▼
kbs_index
 │
 ▼
kbs_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
