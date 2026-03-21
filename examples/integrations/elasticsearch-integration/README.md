# Elasticsearch Integration in Java Using Conductor

It's 3 AM and your production service is throwing 500 errors. You open Kibana to search the logs from the traffic spike at 2:47 AM, and they're not there. The log pipeline silently dropped events when the indexing queue backed up during the load spike. The exact 8 minutes you need to diagnose the incident are a blank hole in your search index. You have logs from before the incident and after the recovery, but the critical window: the one that would show you the root cause, was lost because the pipeline had no backpressure handling and no retry on index failures. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate a durable Elasticsearch index-search-aggregate-analyze pipeline with retries and full observability.

## Indexing, Searching, and Analyzing Data in Elasticsearch

A common Elasticsearch pattern involves indexing a document, searching for related content, aggregating the results to find patterns, and drawing insights from the aggregations. Each step depends on the previous one. You cannot search before indexing, and you cannot aggregate without search hits. If the indexing step fails or the search returns no results, downstream steps need to handle it gracefully.

Without orchestration, you would chain Elasticsearch REST calls manually and manage index names, document IDs, and hit lists between steps. Conductor sequences the pipeline and passes these values automatically via JSONPath.

## The Solution

**You just write the Elasticsearch workers. Document indexing, search execution, aggregation, and insight analysis. Conductor handles document-to-analysis sequencing, index operation retries, and document ID routing between pipeline stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers form the search pipeline: IndexDocWorker writes documents, SearchWorker executes queries against the index, AggregateWorker runs faceted analysis, and AnalyzeWorker produces analytical insights from the aggregation results.

| Worker | Task | What It Does |
|---|---|---|
| **IndexDocWorker** | `els_index_doc` | Indexes a document in Elasticsearch. writes the document to the specified index and returns the document ID |
| **SearchWorker** | `els_search` | Searches the index. executes the search query against the indexed documents and returns matching hits |
| **AggregateWorker** | `els_aggregate` | Aggregates search results: runs aggregation queries (terms, histogram, stats) on the search hits to find patterns and distributions |
| **AnalyzeWorker** | `els_analyze` | Analyzes aggregated results. produces analytical insights and summaries from the aggregation data |

### The Workflow

```
els_index_doc
 │
 ▼
els_search
 │
 ▼
els_aggregate
 │
 ▼
els_analyze

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
