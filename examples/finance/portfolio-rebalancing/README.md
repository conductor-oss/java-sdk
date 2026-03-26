# Portfolio Rebalancing

Portfolio rebalancing workflow that analyzes drift, determines trades, executes, verifies, and reports.

**Input:** `portfolioId`, `accountId`, `strategy` | **Timeout:** 60s

## Pipeline

```
prt_analyze_drift
    │
prt_determine_trades
    │
prt_execute_trades
    │
prt_verify
    │
prt_report
```

## Workers

**AnalyzeDriftWorker** (`prt_analyze_drift`)

Reads `portfolioId`, `strategy`. Outputs `driftAnalysis`, `maxDrift`, `rebalanceNeeded`.

**DetermineTradesWorker** (`prt_determine_trades`)

```java
trades.add(Map.of("asset", a.get("asset"), "action", drift > 0 ? "SELL" : "BUY", "percentChange", Math.abs(drift)));
```

Reads `driftAnalysis`. Outputs `trades`, `taxLotMethod`.

**ExecuteTradesWorker** (`prt_execute_trades`)

```java
for (int i = 0; i < trades.size(); i++) {
```

Reads `accountId`, `trades`. Outputs `executedTrades`, `tradeCount`.

**ReportWorker** (`prt_report`)

Reads `tradesExecuted`, `verified`. Outputs `reportId`, `generatedAt`.

**VerifyWorker** (`prt_verify`)

```java
boolean allFilled = trades.stream().allMatch(t -> Boolean.TRUE.equals(t.get("filled")));
```

Reads `executedTrades`. Outputs `verified`, `newAllocations`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
