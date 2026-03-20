# Nutrition Tracking in Java with Conductor

Tracks a user's nutritional intake: logging meals, looking up calories and macros, calculating daily totals against goals, and generating a nutrition report. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## The Problem

You need to track a user's nutritional intake for a meal. The workflow logs what foods were consumed, looks up the nutritional information (calories, macros, micronutrients) for each food item, calculates daily totals by combining the meal with previous meals that day, and generates a nutrition report. Tracking without accurate nutritional data gives users a false sense of their intake; not calculating daily totals means missing calorie or macro targets.

Without orchestration, you'd build a single tracking service that records food entries, queries a nutrition database, accumulates daily totals, and renders reports .  manually handling foods not found in the database, retrying failed nutrition API lookups, and managing timezone-aware daily boundaries.

## The Solution

**You just write the meal logging, nutrient lookup, daily total calculation, and nutrition report generation logic. Conductor handles ingredient lookup retries, macro aggregation, and nutritional audit trails.**

Each nutrition concern is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of executing them in order (log meal, lookup nutrition, calculate daily totals, generate report), retrying if the nutrition database API is unavailable, tracking every meal entry, and resuming from the last step if the process crashes. You get all of that for free, without writing a single line of orchestration code.

### What You Write: Workers

Ingredient logging, macro calculation, meal scoring, and report generation workers each contribute one layer of nutritional analysis.

| Worker | Task | What It Does |
|---|---|---|
| **CalculateDailyWorker** | `nut_calculate_daily` | Calculates daily totals for calories, protein, carbs, and fat, and compares against the calorie goal |
| **LogMealWorker** | `nut_log_meal` | Logs the meal type and foods consumed for the user and assigns a meal ID |
| **LookupNutritionWorker** | `nut_lookup_nutrition` | Looks up nutritional data (calories, protein, carbs, fat, fiber) for the logged foods |
| **ReportWorker** | `nut_report` | Generates a daily nutrition report with calories consumed, goal, remaining, and on-track status |

Workers simulate food service operations .  order processing, kitchen routing, delivery coordination ,  with realistic outputs. Replace with real POS and delivery integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
nut_log_meal
    │
    ▼
nut_lookup_nutrition
    │
    ▼
nut_calculate_daily
    │
    ▼
nut_report
```

## Example Output

```
=== Example 739: Nutrition Tracking ===

Step 1: Registering task definitions...
  Registered: nut_log_meal, nut_lookup_nutrition, nut_calculate_daily, nut_report

Step 2: Registering workflow 'nutrition_tracking_739'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [calc] Calculating daily totals
  [log] Logging
  [lookup] Looking up nutrition for foods
  [report] Daily intake report generated

  Status: COMPLETED

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
java -jar target/nutrition-tracking-1.0.0.jar
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
java -jar target/nutrition-tracking-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow nutrition_tracking_739 \
  --version 1 \
  --input '{"userId": "USR-55", "USR-55": "mealType", "mealType": "lunch", "lunch": "foods", "foods": ["item-1", "item-2", "item-3"]}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w nutrition_tracking_739 -s COMPLETED -c 5
```

## How to Extend

Swap each worker for your real nutrition tools .  a food database API like Nutritionix for calorie lookups, your health tracking platform for goal comparison, your reporting engine for nutrition summaries, and the workflow runs identically in production.

- **Meal logger**: capture food entries via barcode scanning (Open Food Facts API), photo recognition (Nutritionix, CalorieAI), or manual search
- **Nutrition lookup**: query the USDA FoodData Central API, Nutritionix, or your custom food database for accurate nutritional data
- **Daily calculator**: accumulate macros and micros across all meals, compare against personalized goals (TDEE, macro splits), and flag overages
- **Report generator**: produce daily/weekly nutrition summaries with charts for calories, macros, and nutrient adequacy

Swap your ingredient database or macro calculator and the tracking pipeline stays intact.

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
nutrition-tracking/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/nutritiontracking/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── NutritionTrackingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CalculateDailyWorker.java
│       ├── LogMealWorker.java
│       ├── LookupNutritionWorker.java
│       └── ReportWorker.java
└── src/test/java/nutritiontracking/workers/
    ├── LogMealWorkerTest.java
    └── ReportWorkerTest.java
```
