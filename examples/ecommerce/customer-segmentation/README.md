# Customer Segmentation in Java Using Conductor :  Collect Data, Cluster, Label Segments, Target

Customer segmentation: collect data, cluster, label segments, target. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers .  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## One-Size-Fits-All Marketing Wastes Budget

Sending the same promotion to all customers means high-value VIPs get the same generic email as price-sensitive bargain hunters. Customer segmentation groups customers by behavior .  frequency of purchase, average order value, product preferences, engagement level ,  so each group gets tailored messaging.

The segmentation pipeline collects raw behavioral data, applies clustering algorithms (k-means, RFM analysis) to identify natural groupings, labels each cluster with human-readable descriptions ("High-value loyalists", "Price-sensitive browsers", "At-risk churners"), and generates segment-specific marketing strategies. If the clustering step reveals an unexpected segment, the labeling and targeting steps need to adapt. Each run produces different segments as customer behavior evolves.

## The Solution

**You just write the data collection, clustering, segment labeling, and campaign targeting logic. Conductor handles clustering retries, pipeline ordering, and segment tracking across campaign runs.**

`CollectDataWorker` gathers customer behavioral data .  purchase frequency, average order value, recency of last purchase, browsing patterns, and engagement metrics ,  for the specified dataset. `ClusterWorker` applies clustering algorithms to group customers by behavioral similarity, producing segment assignments with cluster centroids. `LabelSegmentsWorker` analyzes each cluster's characteristics and assigns descriptive labels ,  "VIP loyalists", "New high-potential", "Dormant at-risk", "Price-sensitive active". `TargetWorker` generates segment-specific marketing strategies ,  retention offers for at-risk segments, upsell campaigns for high-potential segments, loyalty rewards for VIPs. Conductor records each segmentation run for trend analysis across runs.

### What You Write: Workers

Data collection, clustering, segment labeling, and targeting workers form a pipeline where each step refines customer groups without coupling to the others.

| Worker | Task | What It Does |
|---|---|---|
| **ClusterWorker** | `seg_cluster` | Running k-means with k= |
| **CollectDataWorker** | `seg_collect_data` | Loading dataset |
| **LabelSegmentsWorker** | `seg_label_segments` | Labeling segments based on centroid characteristics |
| **TargetWorker** | `seg_target` | Performs the target operation |

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
seg_collect_data
    │
    ▼
seg_cluster
    │
    ▼
seg_label_segments
    │
    ▼
seg_target
```

## Example Output

```
=== Example 464: Customer Segmentatio ===

Step 1: Registering task definitions...
  Registered: seg_collect_data, seg_cluster, seg_label_segments, seg_target

Step 2: Registering workflow 'customer_segmentation_workflow'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [cluster] Running k-means with k=
  [collect] Loading dataset
  [label] Labeling segments based on centroid characteristics
  [target] Creating campaigns for

  Status: COMPLETED
  Output: {clusters=..., customers=..., totalCustomers=..., segments=...}

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
java -jar target/customer-segmentation-1.0.0.jar
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
java -jar target/customer-segmentation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow customer_segmentation_workflow \
  --version 1 \
  --input '{"datasetId": "DS-2024Q1", "DS-2024Q1": "numSegments", "numSegments": 3}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w customer_segmentation_workflow -s COMPLETED -c 5
```

## How to Extend

Connect each worker to your real marketing stack. Segment or Amplitude for behavioral data, your ML pipeline for clustering, HubSpot for targeted campaigns, and the workflow runs identically in production.

- **CollectDataWorker** (`seg_collect_data`): query real customer data from Shopify Analytics, Google Analytics 4, or a data warehouse (BigQuery, Snowflake, Redshift) for behavioral metrics
- **ClusterWorker** (`seg_cluster`): implement RFM (Recency, Frequency, Monetary) analysis, k-means clustering via Apache Spark MLlib, or use pre-built segmentation from Segment.com or Amplitude
- **LabelSegmentsWorker** (`seg_label_segments`): assign human-readable names and characteristics to each cluster (e.g., "High-Value Loyalists," "At-Risk Churners," "Bargain Hunters") based on cluster centroids and feature distributions
- **TargetWorker** (`seg_target`): generate campaigns in Mailchimp, Klaviyo, or Braze with segment-specific content, offers, and send-time optimization based on each segment's engagement patterns

Upgrade your clustering algorithm or targeting platform and the segmentation pipeline stays unchanged.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
customer-segmentation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/customersegmentation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CustomerSegmentationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClusterWorker.java
│       ├── CollectDataWorker.java
│       ├── LabelSegmentsWorker.java
│       └── TargetWorker.java
└── src/test/java/customersegmentation/workers/
    ├── ClusterWorkerTest.java        # 2 tests
    ├── CollectDataWorkerTest.java        # 2 tests
    ├── LabelSegmentsWorkerTest.java        # 2 tests
    └── TargetWorkerTest.java        # 2 tests
```
