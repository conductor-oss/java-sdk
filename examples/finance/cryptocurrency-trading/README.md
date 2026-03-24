# Cryptocurrency Trading in Java with Conductor

Crypto trading: monitor market, analyze signals, SWITCH(buy/sell/hold), confirm.

## The Problem

You need to execute a cryptocurrency trading strategy. The workflow monitors market data for a trading pair (e.g., BTC/USD), analyzes technical signals (moving averages, RSI, volume), makes a buy/sell/hold decision based on the strategy, and executes the trade with confirmation. Missing a trading signal in a volatile market means lost opportunity; executing without signal analysis means trading blind.

Without orchestration, you'd build a trading bot with a polling loop that fetches prices, runs technical analysis, executes trades, and confirms. manually handling exchange API rate limits, retrying failed orders, managing the state machine for open orders, and logging every decision for portfolio performance analysis.

## The Solution

**You just write the crypto trading workers. Market monitoring, signal analysis, buy/sell/hold routing, and trade confirmation. Conductor handles conditional SWITCH routing for buy, sell, and hold decisions, automatic retries on exchange API failures, and complete trade decision logging.**

Each trading concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of monitoring the market, analyzing signals, routing via a SWITCH task to buy/sell/hold, executing the trade, and confirming, retrying failed exchange API calls, tracking every trading decision, and resuming from the last step if the process crashes.

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

Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
