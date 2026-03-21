# Feature Engineering in Java Using Conductor :  Feature Extraction, Transformation, Normalization, and Validation

A Java Conductor workflow example for ML feature engineering: extracting raw features from source data, applying transformations (log, polynomial, ratio-based derived features), normalizing numeric features to a [0,1] range via min-max scaling, and validating the final feature set for null values and range compliance before model training. Uses [Conductor](https://github.

## The Problem

Before your ML model can train, raw data needs to become features. That means extracting useful signals from raw fields (parsing dates into day-of-week, computing ratios from absolute values), transforming them (log transforms for skewed distributions, polynomial features for capturing non-linear relationships), normalizing to a consistent [0,1] range so gradient-based models converge properly, and validating that no feature has null values or falls outside the expected range. Each step depends on the previous one: you can't normalize features that haven't been transformed, and you can't validate until normalization is complete.

Without orchestration, you'd build a monolithic feature pipeline that extracts, transforms, normalizes, and validates in one pass. If the normalization step reveals a data issue, you'd re-run everything. There's no record of what the raw features looked like before transformation. Adding a new derived feature means modifying tightly coupled code with no visibility into which step is slow or producing unexpected distributions.

## The Solution

**You just write the feature extraction, transformation, normalization, and validation workers. Conductor handles the sequential feature pipeline, per-step retries, and tracking of feature counts and statistics at every stage for experiment reproducibility.**

Each stage of the feature pipeline is a simple, independent worker. The extractor computes raw features from source data. The transformer adds derived features (log, polynomial, ratio). The normalizer scales all features to [0,1] using min-max normalization. The validator checks for null values and confirms all features are within the expected range. Conductor executes them in sequence, passes the evolving feature matrix between steps, retries if a step fails, and tracks feature counts and statistics at every stage. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers handle the ML feature pipeline: extracting raw features from source data, applying log/polynomial/ratio transformations, normalizing values to [0,1] via min-max scaling, and validating the final feature set for nulls and range compliance.

| Worker | Task | What It Does |
|---|---|---|
| **ExtractFeaturesWorker** | `fe_extract_features` | Extracts raw features from input records. |
| **NormalizeFeaturesWorker** | `fe_normalize_features` | Min-max normalizes features to [0,1] range. |
| **TransformFeaturesWorker** | `fe_transform_features` | Transforms features by adding derived features (log, polynomial, ratio). |
| **ValidateFeaturesWorker** | `fe_validate_features` | Validates normalized features are in [0,1] range with no nulls. |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

### The Workflow

```
fe_extract_features
    │
    ▼
fe_transform_features
    │
    ▼
fe_normalize_features
    │
    ▼
fe_validate_features

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
java -jar target/feature-engineering-1.0.0.jar

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
java -jar target/feature-engineering-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow feature_engineering \
  --version 1 \
  --input '{"rawData": {"key": "value"}, "featureConfig": "sample-featureConfig"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w feature_engineering -s COMPLETED -c 5

```

## How to Extend

Connect the extractor to your feature store or Spark MLlib, implement real normalization with saved scaler parameters, and the ML feature pipeline runs unchanged.

- **ExtractFeaturesWorker** → connect to real data sources (feature stores, data warehouses, training data lakes) and compute features using libraries like Apache Spark MLlib or Tribuo
- **TransformFeaturesWorker** → implement real transformations (one-hot encoding for categoricals, TF-IDF for text, embedding lookups for IDs, interaction terms)
- **NormalizeFeaturesWorker** → use real normalization strategies (z-score standardization, min-max scaling, robust scaling for outlier-heavy data) with saved scaler parameters for inference
- **ValidateFeaturesWorker** → check feature distributions against training baselines, detect data drift, validate feature importance rankings, and flag anomalous feature values

Adding new derived features or switching normalization strategies inside any worker leaves the extract-transform-normalize-validate pipeline intact, as long as the feature matrix format is preserved.

**Add new stages** by inserting tasks in `workflow.json`, for example, a feature selection step that drops low-importance features, a dimensionality reduction step (PCA, t-SNE), or a feature store write step that saves computed features for reuse across models.

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
feature-engineering/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/featureengineering/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── FeatureEngineeringExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExtractFeaturesWorker.java
│       ├── NormalizeFeaturesWorker.java
│       ├── TransformFeaturesWorker.java
│       └── ValidateFeaturesWorker.java
└── src/test/java/featureengineering/workers/
    ├── ExtractFeaturesWorkerTest.java        # 8 tests
    ├── NormalizeFeaturesWorkerTest.java        # 8 tests
    ├── TransformFeaturesWorkerTest.java        # 9 tests
    └── ValidateFeaturesWorkerTest.java        # 8 tests

```
