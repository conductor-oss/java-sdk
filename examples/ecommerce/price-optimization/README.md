# Price Optimization in Java Using Conductor :  Collect Market Data, Analyze Demand, Optimize, Update Prices

A Java Conductor workflow example demonstrating Price Optimization. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## Static Prices Leave Money on the Table

Your competitor just dropped their price by 15%. Your demand hasn't changed yet, but it will. A product priced at $49.99 might sell 100 units/week, but at $44.99 it might sell 150 units. increasing total revenue despite the lower price. Or at $54.99 it might sell 80 units with higher margins. The optimal price depends on competitor pricing, demand elasticity, inventory levels, and margin targets.

Dynamic pricing requires current market data (what are competitors charging right now?), demand analysis (how does price affect sales volume for this product?), optimization (what price maximizes the chosen objective. revenue, profit, or market share?), and execution (updating the price in the catalog without disrupting active carts). Each step uses different data sources and algorithms, and the pipeline should run regularly to keep prices competitive.

## The Solution

**You just write the market data collection, demand analysis, price optimization, and catalog update logic. Conductor handles data pipeline retries, model execution ordering, and pricing decision audit trails.**

`CollectMarketDataWorker` gathers competitor prices, market trends, and current inventory levels for the product. `AnalyzeDemandWorker` computes demand elasticity from historical sales data. how sensitive is demand to price changes for this product category? `OptimizePriceWorker` calculates the optimal price point using the market data, demand curve, margin constraints, and the chosen optimization objective (maximize revenue, profit, or unit volume). `UpdatePriceWorker` applies the new price to the product catalog and records the price change with justification. Conductor records every pricing decision with its inputs for price audit trails.

### What You Write: Workers

Data ingestion, competitor analysis, demand forecasting, and pricing workers each contribute one signal to the final price recommendation.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeDemandWorker** | `prz_analyze_demand` | Analyzes demand for a product: demand score, elasticity, seasonal factor, forecasted demand. |
| **CollectMarketDataWorker** | `prz_collect_market_data` | Collects market data: competitor prices, market trend, average market price, supply level. |
| **OptimizePriceWorker** | `prz_optimize_price` | Optimize Price. Computes and returns new price, adjustment percent, confidence |
| **UpdatePriceWorker** | `prz_update_price` | Updates the product price and computes the price change delta. |

Workers implement e-commerce operations. payment processing, inventory checks, shipping,  with realistic outputs so you can run the full order flow. Replace with real service integrations and the workflow stays the same.

### The Workflow

```
prz_collect_market_data
    │
    ▼
prz_analyze_demand
    │
    ▼
prz_optimize_price
    │
    ▼
prz_update_price

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
java -jar target/price-optimization-1.0.0.jar

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
java -jar target/price-optimization-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow price_optimization_workflow \
  --version 1 \
  --input '{"productId": "TEST-001", "currentPrice": 100, "category": "general"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w price_optimization_workflow -s COMPLETED -c 5

```

## How to Extend

Swap each worker for your real pricing tools. competitor scraping APIs for market data, your demand forecasting model for elasticity analysis, your catalog API for price updates, and the workflow runs identically in production.

- **CollectMarketDataWorker** (`prz_collect_market_data`): scrape competitor prices via Bright Data or Oxylabs, pull market trends from Google Trends API, and check inventory levels from your WMS
- **AnalyzeDemandWorker** (`prz_analyze_demand`): implement price elasticity models using historical sales data from your data warehouse, or use Prophet/ARIMA for demand forecasting
- **UpdatePriceWorker** (`prz_update_price`): update prices via Shopify Product API, Amazon SP-API, or your catalog database, with safeguards against extreme price changes (max % change per update)

Plug in a different demand model or competitor data source and the pricing pipeline incorporates it transparently.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
price-optimization/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/priceoptimization/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PriceOptimizationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeDemandWorker.java
│       ├── CollectMarketDataWorker.java
│       ├── OptimizePriceWorker.java
│       └── UpdatePriceWorker.java
└── src/test/java/priceoptimization/workers/
    ├── AnalyzeDemandWorkerTest.java        # 8 tests
    ├── CollectMarketDataWorkerTest.java        # 8 tests
    ├── OptimizePriceWorkerTest.java        # 8 tests
    └── UpdatePriceWorkerTest.java        # 8 tests

```
