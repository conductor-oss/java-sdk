# Sentiment Analysis in Java with Conductor :  Preprocess, Analyze, Classify, and Aggregate Customer Sentiment

A Java Conductor workflow that analyzes sentiment in customer text. preprocessing the input (cleaning, normalizing, tokenizing), running sentiment analysis to score each text, classifying the overall sentiment as positive/negative/neutral, and aggregating results across multiple texts for trend reporting. Given `texts` and a `source` identifier, the pipeline produces cleaned text, sentiment scores, classifications, and aggregate metrics. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the four-step sentiment pipeline.

## Understanding How Customers Feel at Scale

One angry email is anecdotal. A thousand support tickets with declining sentiment is a trend. Analyzing customer sentiment manually does not scale. you need to preprocess the text (remove noise, normalize formatting), score sentiment for each piece of text, classify it into actionable categories, and aggregate the results to spot trends. Individual scores are useful for routing (flag negative feedback for urgent follow-up), but the aggregate view reveals whether satisfaction is improving or declining over time.

This workflow processes a batch of customer texts through the sentiment pipeline. The preprocessor cleans and normalizes the text (removing HTML, fixing encoding, normalizing case). The analyzer scores each text for sentiment polarity and intensity. The classifier maps scores to categories (positive, negative, neutral) with confidence levels. The aggregator computes batch-level metrics. average sentiment, distribution across categories, and trend indicators.

## The Solution

**You just write the preprocessing, scoring, classification, and aggregation workers. Conductor handles the sentiment pipeline and batch data flow.**

Four workers form the sentiment pipeline. preprocessing, analysis, classification, and aggregation. The preprocessor cleans raw text. The analyzer scores sentiment. The classifier labels each text. The aggregator computes batch metrics and trends. Conductor sequences the four steps and passes cleaned text, scores, classifications, and aggregate metrics between them via JSONPath.

### What You Write: Workers

PreprocessWorker cleans and normalizes text, AnalyzeWorker scores sentiment polarity, ClassifyWorker labels each text as positive/negative/neutral, and AggregateWorker computes batch-level metrics and trend indicators.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateWorker** | `snt_aggregate` | Computes batch-level sentiment metrics: overall polarity, positive/negative/neutral distribution, and trend indicators. |
| **AnalyzeWorker** | `snt_analyze` | Scores each text for sentiment polarity and intensity, producing per-text sentiment values. |
| **ClassifyWorker** | `snt_classify` | Maps sentiment scores to labels (positive, negative, neutral) with confidence levels. |
| **PreprocessWorker** | `snt_preprocess` | Cleans and normalizes raw text: lowercasing, stopword removal, and encoding normalization. |

Workers implement domain operations. lead scoring, contact enrichment, deal updates,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
snt_preprocess
    │
    ▼
snt_analyze
    │
    ▼
snt_classify
    │
    ▼
snt_aggregate

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
java -jar target/sentiment-analysis-1.0.0.jar

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
java -jar target/sentiment-analysis-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow snt_sentiment_analysis \
  --version 1 \
  --input '{"texts": "Process this order for customer C-100", "source": "api"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w snt_sentiment_analysis -s COMPLETED -c 5

```

## How to Extend

Each worker handles one sentiment step. connect your NLP service (AWS Comprehend, Google NLP, MonkeyLearn) for scoring and your analytics platform for trend aggregation, and the sentiment workflow stays the same.

- **AggregateWorker** (`snt_aggregate`): push aggregated sentiment metrics to dashboards (Grafana, Datadog) or data warehouses (BigQuery)
- **AnalyzeWorker** (`snt_analyze`): swap in a real NLP model (VADER, TextBlob) or LLM for more nuanced sentiment scoring
- **ClassifyWorker** (`snt_classify`): use fine-tuned sentiment classifiers (Hugging Face transformers) for domain-specific accuracy

Replace the simulated analyzer with a real NLP model and the sentiment scoring pipeline with batch aggregation keeps running as defined.

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
sentiment-analysis/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/sentimentanalysis/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── SentimentAnalysisExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AggregateWorker.java
│       ├── AnalyzeWorker.java
│       ├── ClassifyWorker.java
│       └── PreprocessWorker.java
└── src/test/java/sentimentanalysis/workers/
    ├── AggregateWorkerTest.java        # 2 tests
    ├── AnalyzeWorkerTest.java        # 2 tests
    ├── ClassifyWorkerTest.java        # 2 tests
    └── PreprocessWorkerTest.java        # 2 tests

```
