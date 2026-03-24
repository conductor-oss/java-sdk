# Elasticsearch Integration

Orchestrates elasticsearch integration through a multi-stage Conductor workflow.

**Input:** `indexName`, `document`, `searchQuery` | **Timeout:** 60s

## Pipeline

```
els_index_doc
    │
els_search
    │
els_aggregate
    │
els_analyze
```

## Workers

**AggregateWorker** (`els_aggregate`): Aggregates search results.

Outputs `aggregations`, `avgScore`, `topCategory`.

**AnalyzeWorker** (`els_analyze`): Analyzes aggregated results.

Reads `totalHits`. Outputs `insights`, `analyzedAt`.

**IndexDocWorker** (`els_index_doc`): Indexes a document in Elasticsearch.

```java
String documentId = "doc-" + Long.toString(System.currentTimeMillis(), 36);
```

Reads `indexName`. Outputs `documentId`, `result`, `version`.

**SearchWorker** (`els_search`): Searches documents in Elasticsearch.

Reads `indexName`, `query`. Outputs `hits`, `totalHits`, `took`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
