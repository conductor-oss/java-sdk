# Text Classification in Java with Conductor

A Java Conductor workflow that classifies text into categories .  preprocessing the input, extracting feature vectors, predicting the category, and scoring prediction confidence with a reliability check. Given raw text and a set of categories, the pipeline returns the predicted category, confidence score, and reliability assessment. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the preprocess-extract-classify-confidence pipeline.

## Sorting Text into Categories Accurately

Incoming text .  support tickets, emails, chat messages ,  needs to be routed to the right team or tagged with the right label. Manual tagging does not scale, and a single-step classifier misses nuance. Reliable classification requires preprocessing (cleaning noise, normalizing formatting), extracting meaningful features, predicting the category, and then scoring the prediction's confidence so low-confidence results can be flagged for human review.

This workflow classifies a single piece of text. The preprocessor cleans and normalizes the input. The feature extractor produces n-grams, keyword counts, and statistical features. The classifier predicts the most likely category from the feature vector. The confidence scorer evaluates the prediction's reliability by checking the margin between the top category and runner-up. Each step's output feeds the next .  cleaned text feeds feature extraction, features feed classification, and the classification feeds confidence scoring.

## The Solution

**You just write the preprocessing, feature-extraction, classification, and confidence-scoring workers. Conductor handles the classification pipeline and feature flow.**

Each worker handles one CRM operation. Conductor manages the customer lifecycle pipeline, assignment routing, follow-up scheduling, and activity tracking.

### What You Write: Workers

PreprocessWorker cleans raw input, ExtractFeaturesWorker produces n-grams and keyword counts, ClassifyWorker predicts the category, and ConfidenceWorker flags low-confidence results for human review.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyWorker** | `txc_classify` | Predicts the most likely category from the feature vector (e.g., "technology"). |
| **ConfidenceWorker** | `txc_confidence` | Scores prediction confidence by computing the margin between the top and runner-up categories; flags low-confidence results. |
| **ExtractFeaturesWorker** | `txc_extract_features` | Extracts n-grams, keyword counts, and statistical features from the cleaned text. |
| **PreprocessWorker** | `txc_preprocess` | Cleans raw text by normalizing whitespace, lowercasing, and removing noise characters. |

Workers implement domain operations .  lead scoring, contact enrichment, deal updates ,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
txc_preprocess
    │
    ▼
txc_extract_features
    │
    ▼
txc_classify
    │
    ▼
txc_confidence
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
java -jar target/text-classification-1.0.0.jar
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
java -jar target/text-classification-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow txc_text_classification \
  --version 1 \
  --input '{"text": "test-value", "categories": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w txc_text_classification -s COMPLETED -c 5
```

## How to Extend

Each worker handles one classification step .  connect your ML model (scikit-learn, Hugging Face, AWS Comprehend) for prediction and your routing system for low-confidence escalation, and the classification workflow stays the same.

- **ClassifyWorker** (`txc_classify`): swap in a fine-tuned BERT classifier or LLM for example-grade text classification
- **ConfidenceWorker** (`txc_confidence`): use model probability outputs or calibrated scores for real confidence estimation
- **ExtractFeaturesWorker** (`txc_extract_features`): integrate with embedding APIs (OpenAI, Cohere) or TF-IDF libraries for real feature extraction

Wire up a production ML classifier and the preprocess-extract-classify-score pipeline continues without any workflow definition changes.

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
text-classification/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/textclassification/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TextClassificationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClassifyWorker.java
│       ├── ConfidenceWorker.java
│       ├── ExtractFeaturesWorker.java
│       └── PreprocessWorker.java
└── src/test/java/textclassification/workers/
    ├── ClassifyWorkerTest.java        # 2 tests
    ├── ConfidenceWorkerTest.java        # 2 tests
    ├── ExtractFeaturesWorkerTest.java        # 2 tests
    └── PreprocessWorkerTest.java        # 2 tests
```
