# Recommendation Engine in Java Using Conductor :  Collect Behavior, Compute Similarity, Rank, Personalize

A Java Conductor workflow example demonstrating Recommendation Engine. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Generic "Popular Items" Lists Don't Convert

Showing every customer the same "Top Sellers" list misses the point of personalization. A customer who buys hiking gear should see trail shoes and backpacks, not kitchen appliances. Collaborative filtering finds patterns: "Customers who bought X also bought Y." Content-based filtering uses item attributes: "This hiking boot is similar to the one you viewed." A good recommendation engine combines both.

The pipeline collects behavioral signals (what did this user view, purchase, and wishlist?), computes similarity scores between items (based on co-purchase patterns and item attributes), ranks candidates by predicted relevance for this specific user, and personalizes the final list (boosting items matching the user's current session context). Each step narrows the focus from millions of products to a ranked list of 10-20 recommendations.

## The Solution

**You just write the behavior collection, similarity computation, candidate ranking, and personalization logic. Conductor handles scoring retries, candidate pipeline sequencing, and recommendation audit trails.**

`CollectBehaviorWorker` gathers the user's recent activity .  page views, purchases, search queries, wishlist additions, and cart interactions ,  as behavioral signals. `ComputeSimilarityWorker` calculates item-to-item similarity using collaborative filtering (co-purchase patterns) and content-based features (category, price range, attributes). `RankCandidatesWorker` scores candidate products by predicted relevance for this user, combining collaborative and content-based signals. `PersonalizeWorker` adjusts the final ranking based on context ,  boosting items matching the current session, applying diversity rules (don't show 5 similar items), and filtering already-purchased items. Conductor records which recommendations were generated and, combined with click/purchase tracking, enables recommendation quality measurement.

### What You Write: Workers

Profile analysis, candidate generation, scoring, and delivery workers each own one layer of the recommendation pipeline.

| Worker | Task | What It Does |
|---|---|---|
| **CollectBehaviorWorker** | `rec_collect_behavior` | Gathers user browsing behavior: viewed products, purchased products, browsing categories, and session count. |
| **ComputeSimilarityWorker** | `rec_compute_similarity` | Computes similarity scores between user behavior and candidate products. |
| **PersonalizeWorker** | `rec_personalize` | Personalizes the top-ranked products for the user's context, returning the top 3 with a reason string. |
| **RankCandidatesWorker** | `rec_rank_candidates` | Ranks candidate products by similarity score and assigns a rank number. |

Workers simulate e-commerce operations .  payment processing, inventory checks, shipping ,  with realistic outputs so you can run the full order flow. Replace with real service integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
rec_collect_behavior
    │
    ▼
rec_compute_similarity
    │
    ▼
rec_rank_candidates
    │
    ▼
rec_personalize
```

## Example Output

```
=== Example 461: Recommendation Engine ===

Step 1: Registering task definitions...
  Registered: rec_collect_behavior, rec_compute_similarity, rec_rank_candidates, rec_personalize

Step 2: Registering workflow 'recommendation_engine_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [collect] Gathering behavior for user
  [similarity] Computing similarity scores for
  [personalize] Personalizing
  [rank] Ranking

  Status: COMPLETED
  Output: {viewedProducts=..., purchasedProducts=..., browsingCategories=..., sessionCount=...}

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
java -jar target/recommendation-engine-1.0.0.jar
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
java -jar target/recommendation-engine-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow recommendation_engine_workflow \
  --version 1 \
  --input '{"userId": "USER-7891", "USER-7891": "context", "context": "product_page", "product_page": "sample-product-page"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w recommendation_engine_workflow -s COMPLETED -c 5
```

## How to Extend

Plug each worker into your real recommendation stack. Segment for behavioral data, Spark MLlib for similarity scoring, Amazon Personalize for ranking, and the workflow runs identically in production.

- **CollectBehaviorWorker** (`rec_collect_behavior`): pull user events from Segment, Amplitude, or Google Analytics 4, or query a clickstream data warehouse for behavioral signals
- **ComputeSimilarityWorker** (`rec_compute_similarity`): use Apache Mahout or Spark MLlib for collaborative filtering at scale, or precomputed item embeddings from a recommendation model (e.g., Two-Tower model)
- **PersonalizeWorker** (`rec_personalize`): integrate with Amazon Personalize, Algolia Recommend, or Recombee for production-grade personalization with real-time model updates

Swap your scoring model or candidate source and the recommendation pipeline structure remains the same.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):


## Project Structure

```
recommendation-engine/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/recommendationengine/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── RecommendationEngineExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CollectBehaviorWorker.java
│       ├── ComputeSimilarityWorker.java
│       ├── PersonalizeWorker.java
│       └── RankCandidatesWorker.java
└── src/test/java/recommendationengine/workers/
    ├── CollectBehaviorWorkerTest.java        # 8 tests
    ├── ComputeSimilarityWorkerTest.java        # 8 tests
    ├── PersonalizeWorkerTest.java        # 8 tests
    └── RankCandidatesWorkerTest.java        # 8 tests
```
