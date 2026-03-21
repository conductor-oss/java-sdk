# Cryptocurrency Trading in Java with Conductor

Crypto trading: monitor market, analyze signals, SWITCH(buy/sell/hold), confirm. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to execute a cryptocurrency trading strategy. The workflow monitors market data for a trading pair (e.g., BTC/USD), analyzes technical signals (moving averages, RSI, volume), makes a buy/sell/hold decision based on the strategy, and executes the trade with confirmation. Missing a trading signal in a volatile market means lost opportunity; executing without signal analysis means trading blind.

Without orchestration, you'd build a trading bot with a polling loop that fetches prices, runs technical analysis, executes trades, and confirms. manually handling exchange API rate limits, retrying failed orders, managing the state machine for open orders, and logging every decision for portfolio performance analysis.

## The Solution

**You just write the crypto trading workers. Market monitoring, signal analysis, buy/sell/hold routing, and trade confirmation. Conductor handles conditional SWITCH routing for buy, sell, and hold decisions, automatic retries on exchange API failures, and complete trade decision logging.**

Each trading concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of monitoring the market, analyzing signals, routing via a SWITCH task to buy/sell/hold, executing the trade, and confirming,  retrying failed exchange API calls, tracking every trading decision, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Six workers drive the trading cycle: MonitorMarketWorker fetches prices, AnalyzeSignalsWorker evaluates technical indicators, SWITCH routes to ExecuteBuyWorker, ExecuteSellWorker, or ExecuteHoldWorker based on the signal, and ConfirmActionWorker records the outcome.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeSignalsWorker** | `cry_analyze_signals` | Analyze Signals. Computes and returns signal, confidence, suggested amount, hold reason |
| **ConfirmActionWorker** | `cry_confirm_action` | Confirm Action. Computes and returns confirmed, portfolio updated, log id |
| **ExecuteBuyWorker** | `cry_execute_buy` | Executes the buy |
| **ExecuteHoldWorker** | `cry_execute_hold` | Executes the hold |
| **ExecuteSellWorker** | `cry_execute_sell` | Executes the sell |
| **MonitorMarketWorker** | `cry_monitor_market` | Monitors the market |

Workers implement financial operations. risk assessment, compliance checks, settlement,  with realistic outputs. Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
cry_monitor_market
    │
    ▼
cry_analyze_signals
    │
    ▼
SWITCH (cry_switch_ref)
    ├── buy: cry_execute_buy
    ├── sell: cry_execute_sell
    ├── hold: cry_execute_hold
    │
    ▼
cry_confirm_action

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
java -jar target/cryptocurrency-trading-1.0.0.jar

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
java -jar target/cryptocurrency-trading-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cryptocurrency_trading_workflow \
  --version 1 \
  --input '{"pair": "sample-pair", "portfolioId": "TEST-001", "strategy": "sample-strategy"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cryptocurrency_trading_workflow -s COMPLETED -c 5

```

## How to Extend

Connect MonitorMarketWorker to your exchange's market data API (Coinbase, Binance), AnalyzeSignalsWorker to your trading strategy engine, and the execution workers to your exchange's order API. The workflow definition stays exactly the same.

- **Market monitor**: connect to exchange WebSocket feeds (Binance, Coinbase, Kraken) for real-time price and orderbook data
- **Signal analyzer**: implement technical indicators (MACD, RSI, Bollinger Bands) using ta4j or custom quantitative models
- **Trade executor**: place orders via exchange APIs (Binance API, Coinbase Pro, Kraken REST) with proper order types (limit, market, stop-loss)
- **Confirmation handler**: verify fills, update portfolio positions, and calculate realized P&L

Swap in real exchange APIs and your trading signal engine while returning the same fields, and the trading workflow: including SWITCH routing, runs without changes.

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
cryptocurrency-trading/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/cryptocurrencytrading/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CryptocurrencyTradingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AnalyzeSignalsWorker.java
│       ├── ConfirmActionWorker.java
│       ├── ExecuteBuyWorker.java
│       ├── ExecuteHoldWorker.java
│       ├── ExecuteSellWorker.java
│       └── MonitorMarketWorker.java
└── src/test/java/cryptocurrencytrading/workers/
    ├── AnalyzeSignalsWorkerTest.java        # 2 tests
    └── ConfirmActionWorkerTest.java        # 2 tests

```
