# Content Recommendation Engine in Java Using Conductor :  History Analysis, Similarity Scoring, Ranking, Filtering, and Serving

A Java Conductor workflow example that orchestrates a content recommendation pipeline .  analyzing user viewing history and preferences (liked items, top categories, activity scores), computing content similarity scores, ranking candidates by relevance, applying business filters (already-viewed removal, diversity rules), and serving personalized recommendations with tracking IDs for A/B evaluation. Uses [Conductor](https://github.## Why Recommendation Pipelines Need Orchestration

Building a recommendation involves a multi-stage pipeline where each stage refines the previous one's output. You analyze the user's history .  viewed items, liked items, top categories, activity score. You compute similarity scores between the user's preferences and the content catalog, generating candidate items with relevance scores. You rank those candidates using a learned ranking model. You apply business filters ,  removing already-viewed content, enforcing category diversity, respecting content freshness rules. Finally, you serve the filtered, ranked list to the user with sub-200ms response time.

Each stage depends on the previous one .  similarity scoring needs user history, ranking needs similarity scores, filtering needs ranked results. If the history lookup fails, you cannot compute relevant similarities. If you skip filtering, users see content they have already viewed. Without orchestration, you'd build a monolithic recommendation engine that mixes user profiling, embedding lookups, ML inference, and business logic ,  making it impossible to swap your ranking model, test filter rules independently, or measure which stage is the latency bottleneck.

## How This Workflow Solves It

**You just write the recommendation workers. History analysis, similarity scoring, ranking, filtering, and serving. Conductor handles stage sequencing, model endpoint retries, and response-time tracking for latency optimization.**

Each recommendation stage is an independent worker .  analyze history, compute similarity, rank results, apply filters, serve recommendations. Conductor sequences them, passes user profiles and candidate lists between stages, retries if a model endpoint times out, and tracks response time and cache hit rates for performance optimization.

### What You Write: Workers

Five workers build the recommendation pipeline: AnalyzeHistoryWorker profiles user preferences, ComputeSimilarityWorker scores content candidates, RankResultsWorker orders by relevance, ApplyFiltersWorker enforces diversity rules, and ServeRecommendationsWorker delivers personalized results.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeHistoryWorker** | `crm_analyze_history` | Analyzes the history |
| **ApplyFiltersWorker** | `crm_apply_filters` | Applies filters |
| **ComputeSimilarityWorker** | `crm_compute_similarity` | Computes the similarity |
| **RankResultsWorker** | `crm_rank_results` | Ranks results |
| **ServeRecommendationsWorker** | `crm_serve_recommendations` | Handles serve recommendations |

Workers simulate media processing stages .  transcoding, thumbnail generation, metadata extraction ,  with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
crm_analyze_history
    │
    ▼
crm_compute_similarity
    │
    ▼
crm_rank_results
    │
    ▼
crm_apply_filters
    │
    ▼
crm_serve_recommendations
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
java -jar target/content-recommendation-1.0.0.jar
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
java -jar target/content-recommendation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow content_recommendation_workflow \
  --version 1 \
  --input '{"userId": "TEST-001", "contentType": "test-value", "maxResults": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w content_recommendation_workflow -s COMPLETED -c 5
```

## How to Extend

Connect AnalyzeHistoryWorker to your user activity store, ComputeSimilarityWorker to your embedding service, and RankResultsWorker to your ML ranking model. The workflow definition stays exactly the same.

- **AnalyzeHistoryWorker** (`crm_analyze_history`): query your user activity store (Redis, DynamoDB, BigQuery) for viewing history, liked items, top categories, and activity scores
- **ComputeSimilarityWorker** (`crm_compute_similarity`): call your embedding/similarity service (Pinecone, Milvus, custom vector search) to find content items similar to the user's preferences
- **RankResultsWorker** (`crm_rank_results`): invoke your ranking model (TensorFlow Serving, SageMaker endpoint, LightGBM) to score and order candidates by predicted engagement
- **ApplyFiltersWorker** (`crm_apply_filters`): enforce business rules: remove already-viewed items, ensure category diversity, apply editorial boosts, and filter by content freshness
- **ServeRecommendationsWorker** (`crm_serve_recommendations`): return the final recommendation list to the client API with tracking IDs for click-through measurement

Swap any worker for a production embedding service or ranking model while maintaining the output schema, and the recommendation flow stays intact.

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
content-recommendation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/contentrecommendation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ContentRecommendationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeHistoryWorker.java
│       ├── ApplyFiltersWorker.java
│       ├── ComputeSimilarityWorker.java
│       ├── RankResultsWorker.java
│       └── ServeRecommendationsWorker.java
└── src/test/java/contentrecommendation/workers/
    ├── AnalyzeHistoryWorkerTest.java        # 2 tests
    └── ComputeSimilarityWorkerTest.java        # 2 tests
```
