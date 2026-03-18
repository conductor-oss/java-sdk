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

| Worker | Task | What It Does | Real / Simulated |
|---|---|---|---|
| **IndexDocWorker** | `els_index_doc` | Indexes a document in Elasticsearch. writes the document to the specified index and returns the document ID | Simulated, swap in Elasticsearch RestHighLevelClient.index() or the new Java API Client for production |
| **SearchWorker** | `els_search` | Searches the index. executes the search query against the indexed documents and returns matching hits | Simulated, swap in Elasticsearch SearchRequest with your query DSL for production |
| **AggregateWorker** | `els_aggregate` | Aggregates search results: runs aggregation queries (terms, histogram, stats) on the search hits to find patterns and distributions | Simulated, swap in Elasticsearch AggregationBuilders for production |
| **AnalyzeWorker** | `els_analyze` | Analyzes aggregated results. produces analytical insights and summaries from the aggregation data | Simulated, swap in your analytics logic or visualization pipeline for production |

Workers simulate external API calls with realistic response shapes so you can see the integration flow end-to-end. Replace with real API clients, the workflow orchestration and error handling stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

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

## Example Output

```
=== Example 446: Elasticsearch Integratio ===

Step 1: Registering task definitions...
  Registered: els_index_doc, els_search, els_aggregate, els_analyze

Step 2: Registering workflow 'elasticsearch_integration_446'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: 8d413d10-73ac-9997-f3fb-5255f5568bb3

  [index] Indexed doc- into reports-2024
  [search] Query "revenue report" -> 3 hits in reports-2024
  [aggregate] Top category: analytics, avg score: 8.27
  [analyze] Found 


  Status: COMPLETED
  Output: {documentId=<"" + doc>, totalHits=3, topCategory=analytics, insights=Found }

Result: PASSED
```
## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build
```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build
```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/elasticsearch-integration-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |
| `ELASTICSEARCH_URL` | _(none)_ | Elasticsearch cluster URL (e.g., `http://localhost:9200`). Currently unused, all workers run in simulated mode with `` output prefix. Swap in Elasticsearch Java client for production. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/elasticsearch-integration-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow elasticsearch_integration_446 \
  --version 1 \
  --input '{"indexName": "reports-2024", "document": {"title": "Monthly Revenue Report", "category": "finance"}, "searchQuery": "revenue report"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w elasticsearch_integration_446 -s COMPLETED -c 5
```

## How to Extend

Swap in the Elasticsearch Java API Client for indexing, your query DSL for search, and AggregationBuilders for the aggregation step against your real cluster. The workflow definition stays exactly the same.

- **AggregateWorker** (`els_aggregate`): use the Elasticsearch Java client's SearchRequest with AggregationBuilders for real aggregations
- **IndexDocWorker** (`els_index_doc`): use the Elasticsearch Java client's IndexRequest to index real documents
- **SearchWorker** (`els_search`): use the Elasticsearch Java client's SearchRequest with QueryBuilders for real full-text search

Swap each simulation for real Elasticsearch API calls while preserving return fields, and the index-search-analyze pipeline needs no changes.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>
```

## Project Structure

```
elasticsearch-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/elasticsearchintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ElasticsearchIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateWorker.java
│       ├── AnalyzeWorker.java
│       ├── IndexDocWorker.java
│       └── SearchWorker.java
└── src/test/java/elasticsearchintegration/workers/
    ├── AggregateWorkerTest.java        # 2 tests
    ├── AnalyzeWorkerTest.java        # 2 tests
    ├── IndexDocWorkerTest.java        # 2 tests
    └── SearchWorkerTest.java        # 2 tests
```
