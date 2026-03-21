# Price Optimization in Java Using Conductor : Collect Market Data, Analyze Demand, Optimize, Update Prices

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
