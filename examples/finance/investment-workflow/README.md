# Investment Workflow

Investment lifecycle: research, analyze, decide, execute, monitor.

**Input:** `tickerSymbol`, `investorId`, `maxInvestment` | **Timeout:** 60s

## Pipeline

```
ivt_research
    │
ivt_analyze
    │
ivt_decide
    │
ivt_execute
    │
ivt_monitor
```

## Workers

**AnalyzeWorker** (`ivt_analyze`)

Reads `tickerSymbol`. Outputs `riskScore`, `expectedReturn`, `recommendation`, `sharpeRatio`.

**DecideWorker** (`ivt_decide`)

```java
int shares = (int) Math.floor(maxInvest / 185.50);
```

Reads `maxInvestment`, `recommendation`. Outputs `action`, `shares`, `priceLimit`, `orderType`.

**ExecuteWorker** (`ivt_execute`)

Reads `action`, `shares`, `tickerSymbol`. Outputs `tradeId`, `executedPrice`, `totalCost`.

**MonitorWorker** (`ivt_monitor`)

Reads `executedPrice`, `tradeId`. Outputs `monitoringStatus`, `stopLoss`, `takeProfit`, `alertsConfigured`.

**ResearchWorker** (`ivt_research`)

Reads `tickerSymbol`. Outputs `fundamentals`, `marketData`, `sector`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
