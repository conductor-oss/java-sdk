# Cryptocurrency Trading

Crypto trading: monitor market, analyze signals, SWITCH(buy/sell/hold), confirm.

**Input:** `pair`, `portfolioId`, `strategy` | **Timeout:** 60s

## Pipeline

```
cry_monitor_market
    │
cry_analyze_signals
    │
cry_trade_decision [SWITCH]
  ├─ buy: cry_execute_buy
  ├─ sell: cry_execute_sell
  └─ hold: cry_execute_hold
    │
cry_confirm_action
```

## Workers

**AnalyzeSignalsWorker** (`cry_analyze_signals`)

Reads `currentPrice`, `pair`. Outputs `signal`, `confidence`, `suggestedAmount`, `holdReason`, `indicators`.

**ConfirmActionWorker** (`cry_confirm_action`)

Reads `pair`, `signal`. Outputs `confirmed`, `portfolioUpdated`, `logId`.

**ExecuteBuyWorker** (`cry_execute_buy`)

Reads `amount`, `pair`, `price`. Outputs `orderId`, `filledPrice`, `filledAmount`, `fee`.

**ExecuteHoldWorker** (`cry_execute_hold`)

Reads `pair`, `reason`. Outputs `action`, `reason`, `reviewAt`.

**ExecuteSellWorker** (`cry_execute_sell`)

Reads `amount`, `pair`, `price`. Outputs `orderId`, `filledPrice`, `filledAmount`, `fee`.

**MonitorMarketWorker** (`cry_monitor_market`)

Reads `pair`. Outputs `currentPrice`, `change24h`, `volume24h`, `marketData`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
