# A/B Testing Pipeline in Java Using Conductor :  Variant Definition, User Assignment, Metric Collection, and Statistical Analysis

A Java Conductor workflow example that orchestrates an end-to-end A/B test .  defining experiment variants with traffic splits, assigning users to control and treatment groups via random hashing, collecting engagement metrics (clicks, impressions, conversion rates), running statistical significance analysis (p-value, uplift, confidence intervals, effect size), and deciding a winner with a rollout recommendation. Uses [Conductor](https://github.## Why A/B Test Pipelines Need Orchestration

Running a rigorous A/B test involves a strict sequence where each stage depends on the previous one. You define the variants (control vs. treatment) and traffic split percentages. You assign users to groups using deterministic hashing so the same user always sees the same variant. You collect behavioral metrics .  clicks, impressions, conversion rates ,  for each group over the experiment window. You run statistical analysis to compute p-values, measure uplift, and determine whether results are statistically significant at your target confidence level. Finally, you decide a winner and generate a rollout recommendation.

If user assignment fails midway, you need to know which users were already assigned to avoid reassignment that would corrupt your experiment. If metric collection is incomplete, the statistical analysis will produce unreliable results. Without orchestration, you'd build a monolithic experimentation framework that mixes user bucketing, event aggregation, and statistical computation .  making it impossible to swap your randomization strategy, test significance calculations independently, or audit which users were in which group.

## How This Workflow Solves It

**You just write the experimentation workers. Variant definition, user bucketing, metric collection, statistical analysis, and winner selection. Conductor handles experiment sequencing, metric collection retries, and a complete audit trail of every configuration and group assignment.**

Each experiment stage is an independent worker .  define variants, assign users, collect data, analyze results, decide winner. Conductor sequences them, passes variant definitions through user assignment into data collection and analysis, retries if a metric query times out, and maintains a complete audit trail of every experiment configuration, group assignment, and statistical result.

### What You Write: Workers

Five workers drive the experiment lifecycle: DefineVariantsWorker sets up control and treatment groups, AssignUsersWorker buckets participants via hashing, CollectDataWorker gathers engagement metrics, AnalyzeResultsWorker runs statistical tests, and DecideWinnerWorker picks the rollout winner.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeResultsWorker** | `abt_analyze_results` | Analyzes results |
| **AssignUsersWorker** | `abt_assign_users` | Assigns users |
| **CollectDataWorker** | `abt_collect_data` | Collects data |
| **DecideWinnerWorker** | `abt_decide_winner` | Handles decide winner |
| **DefineVariantsWorker** | `abt_define_variants` | Defines variants |

Workers simulate media processing stages .  transcoding, thumbnail generation, metadata extraction ,  with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
abt_define_variants
    │
    ▼
abt_assign_users
    │
    ▼
abt_collect_data
    │
    ▼
abt_analyze_results
    │
    ▼
abt_decide_winner
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
java -jar target/ab-testing-1.0.0.jar
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
java -jar target/ab-testing-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ab_testing_workflow \
  --version 1 \
  --input '{"testId": "TEST-001", "testName": "test", "variantA": "test-value", "variantB": "test-value", "sampleSize": 10}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ab_testing_workflow -s COMPLETED -c 5
```

## How to Extend

Connect AssignUsersWorker to your feature flagging service (LaunchDarkly, Split), CollectDataWorker to your analytics warehouse, and AnalyzeResultsWorker to your statistical analysis engine. The workflow definition stays exactly the same.

- **DefineVariantsWorker** (`abt_define_variants`): register experiment variants in your feature flag system (LaunchDarkly, Optimizely, Unleash) with traffic split percentages and targeting rules
- **AssignUsersWorker** (`abt_assign_users`): implement real user bucketing using consistent hashing against user IDs, with assignment records stored in your experiment database for reproducibility
- **CollectDataWorker** (`abt_collect_data`): query real engagement metrics from your analytics warehouse (BigQuery, Snowflake, Redshift) aggregated by experiment group over the collection window
- **AnalyzeResultsWorker** (`abt_analyze_results`): run real statistical tests (chi-squared, t-test, Bayesian analysis) using Apache Commons Math or a statistics service to compute p-values, confidence intervals, and effect sizes
- **DecideWinnerWorker** (`abt_decide_winner`): apply your decision criteria (minimum sample size, significance threshold, practical significance) and generate a rollout recommendation with the winning variant

Point each worker at your real feature flag system or analytics warehouse while preserving output fields, and the experiment pipeline needs no changes.

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
ab-testing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/abtesting/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AbTestingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeResultsWorker.java
│       ├── AssignUsersWorker.java
│       ├── CollectDataWorker.java
│       ├── DecideWinnerWorker.java
│       └── DefineVariantsWorker.java
└── src/test/java/abtesting/workers/
    ├── AssignUsersWorkerTest.java        # 2 tests
    └── DefineVariantsWorkerTest.java        # 2 tests
```
