# Knowledge Base Sync in Java with Conductor :  Crawl, Extract, Update, Index, and Verify

A Java Conductor workflow that keeps a knowledge base in sync with a source .  crawling the source URL for new and changed content, extracting structured articles, updating the knowledge base records, re-indexing for search, and verifying that the sync completed correctly. Given a `sourceUrl` and `kbId`, the pipeline produces crawl counts, extraction results, update status, index metrics, and verification results. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the five-step sync pipeline.

## Keeping Help Content Up to Date

Knowledge bases go stale. Product features change, documentation gets updated, and new articles are published; but the knowledge base that customers and support agents search still shows last month's content. Syncing manually is tedious and error-prone. A reliable sync pipeline needs to crawl the source for changes, extract the content, update the knowledge base, rebuild the search index, and verify everything landed correctly.

This workflow automates the full sync cycle. The crawler scans the source URL and identifies new or changed pages. The extractor pulls structured content (title, body, categories) from the crawled pages. The updater writes the extracted content to the knowledge base. The indexer rebuilds the search index to include the new content. The verifier checks that the updated articles are accessible and the index returns correct results. Each step depends on the previous one .  you cannot extract from uncrawled pages or index unupdated content.

## The Solution

**You just write the crawling, extraction, updating, indexing, and verification workers. Conductor handles the five-step sync pipeline and data flow.**

Five workers handle the sync lifecycle .  crawling, extraction, updating, indexing, and verification. The crawler discovers changed content at the source. The extractor parses pages into structured articles. The updater writes to the knowledge base. The indexer rebuilds search. The verifier confirms the sync is correct. Conductor sequences all five steps and passes crawled pages, extracted content, and update status between them via JSONPath.

### What You Write: Workers

CrawlWorker discovers changed pages, ExtractWorker parses article content, UpdateWorker writes to the KB, IndexWorker rebuilds search, and VerifyWorker confirms the sync is correct.

| Worker | Task | What It Does |
|---|---|---|
| **CrawlWorker** | `kbs_crawl` | Scans the source URL and discovers new or changed pages to process. |
| **ExtractWorker** | `kbs_extract` | Pulls structured article content (title, body, categories) from crawled pages. |
| **IndexWorker** | `kbs_index` | Rebuilds the search index to include newly updated articles. |
| **UpdateWorker** | `kbs_update` | Writes extracted articles to the knowledge base, tracking new, updated, and deleted counts. |
| **VerifyWorker** | `kbs_verify` | Confirms that all indexed articles are searchable and the sync completed correctly. |

Workers implement domain operations .  lead scoring, contact enrichment, deal updates ,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/knowledge-base-sync-1.0.0.jar
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

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/knowledge-base-sync-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow kbs_knowledge_base_sync \
  --version 1 \
  --input '{"sourceUrl": "https://example.com", "kbId": "TEST-001"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w kbs_knowledge_base_sync -s COMPLETED -c 5
```

## How to Extend

Each worker handles one sync stage .  connect your content source for crawling, your KB platform (Zendesk Guide, Confluence, Notion) for updates, and your search engine (Elasticsearch, Algolia) for indexing, and the sync workflow stays the same.

- **CrawlWorker** (`kbs_crawl`): use a real web crawler (Scrapy, Jsoup) or CMS API to discover changed content
- **ExtractWorker** (`kbs_extract`): integrate with content parsers or an LLM to extract structured articles from raw HTML
- **IndexWorker** (`kbs_index`): connect to Elasticsearch, Algolia, or a vector database for example-grade search indexing

Swap in a real crawler and search indexer and the five-step KB sync pipeline keeps working without any workflow changes.

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
knowledge-base-sync/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/knowledgebasesync/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── KnowledgeBaseSyncExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CrawlWorker.java
│       ├── ExtractWorker.java
│       ├── IndexWorker.java
│       ├── UpdateWorker.java
│       └── VerifyWorker.java
└── src/test/java/knowledgebasesync/workers/
    ├── CrawlWorkerTest.java        # 2 tests
    ├── ExtractWorkerTest.java        # 2 tests
    ├── IndexWorkerTest.java        # 2 tests
    ├── UpdateWorkerTest.java        # 2 tests
    └── VerifyWorkerTest.java        # 2 tests
```
