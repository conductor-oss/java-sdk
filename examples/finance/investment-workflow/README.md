# Investment Workflow in Java with Conductor

Investment lifecycle: research, analyze, decide, execute, monitor. ## The Problem

You need to manage the full investment lifecycle for a security. This means researching the investment opportunity (fundamentals, market conditions), analyzing risk and return potential, making a buy/hold/pass decision, executing the trade if appropriate, and monitoring the position post-investment. Making investment decisions without research leads to uninformed bets; executing without analysis means ignoring risk-return tradeoffs.

Without orchestration, you'd build a single investment platform that pulls market data, runs analysis models, places trades, and monitors positions. manually tracking which opportunities are being researched, retrying failed market data API calls, and logging every decision for compliance with fiduciary duty requirements.

## The Solution

**You just write the investment workers. Opportunity research, risk-return analysis, buy/hold/pass decision, trade execution, and position monitoring. Conductor handles lifecycle ordering, automatic retries when market data APIs time out, and complete decision tracking for fiduciary compliance.**

Each investment concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (research, analyze, decide, execute, monitor), retrying if market data APIs time out, tracking every investment decision with full rationale, and resuming from the last step if the process crashes. ### What You Write: Workers

Five workers manage the investment lifecycle: ResearchWorker gathers fundamentals and market data, AnalyzeWorker evaluates risk-return profiles, DecideWorker makes the buy/hold/pass decision, ExecuteWorker places the trade, and MonitorWorker tracks the position.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeWorker** | `ivt_analyze` | Analyzes risk-return profile for the investment opportunity |
| **DecideWorker** | `ivt_decide` | Decide. Computes and returns action, shares, price limit, order type |
| **ExecuteWorker** | `ivt_execute` | Executes the operation and computes trade id, executed price, total cost |
| **MonitorWorker** | `ivt_monitor` | Monitoring trade |
| **ResearchWorker** | `ivt_research` | Researches the ticker symbol and returns fundamental data (P/E ratio, revenue, earnings growth, dividend yield) and market data (price, volume, beta, 200-day SMA) along with sector classification |

Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
ivt_research
 │
 ▼
ivt_analyze
 │
 ▼
ivt_decide
 │
 ▼
ivt_execute
 │
 ▼
ivt_monitor

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
