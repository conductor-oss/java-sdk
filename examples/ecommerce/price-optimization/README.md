# Price Optimization

Orchestrates price optimization through a multi-stage Conductor workflow.

**Input:** `productId`, `currentPrice`, `category` | **Timeout:** 60s

## Pipeline

```
prz_collect_market_data
    │
prz_analyze_demand
    │
prz_optimize_price
    │
prz_update_price
```

## Workers

**AnalyzeDemandWorker** (`prz_analyze_demand`): Analyzes demand for a product: demand score, elasticity, seasonal factor, forecasted demand.

Reads `productId`. Outputs `demandScore`, `elasticity`, `seasonalFactor`, `forecastedDemand`.

**CollectMarketDataWorker** (`prz_collect_market_data`): Collects market data: competitor prices, market trend, average market price, supply level.

Reads `category`, `productId`. Outputs `competitorPrices`, `marketTrend`, `avgMarketPrice`, `supplyLevel`.

**OptimizePriceWorker** (`prz_optimize_price`): Optimizes the product price based on current price and demand score.

```java
double newPrice = Math.round(currentPrice * (1 + (demandScore - 0.5) * 0.1) * 100.0) / 100.0;
double adjustmentPercent = Math.round(((newPrice - currentPrice) / currentPrice) * 10000.0) / 100.0;
```

Reads `currentPrice`, `demandScore`. Outputs `newPrice`, `adjustmentPercent`, `confidence`.

**UpdatePriceWorker** (`prz_update_price`): Updates the product price and computes the price change delta.

```java
double change = Math.round((newPrice - oldPrice) * 100.0) / 100.0;
```

Reads `currentPrice`, `newPrice`. Outputs `priceChange`, `updated`, `effectiveAt`.

## Tests

**32 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
