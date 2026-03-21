# Campaign Automation in Java with Conductor : Design, Target, Execute, and Measure Marketing Campaigns

## Running a Campaign End-to-End

Marketing campaigns involve a strict sequence: design the creative assets and choose channels, build the target audience within the budget, execute the send across all channels, then measure what worked. Each phase depends on the previous one. you cannot target an audience before the campaign exists, and you cannot measure results before execution. Skipping a step or running them out of order wastes budget and produces unreliable metrics.

This workflow models that lifecycle explicitly. The design step creates a campaign ID with creative assets and channel assignments (email, social, display). The targeting step uses the campaign ID and budget to build an audience segment. The execution step sends to that audience. The measurement step calculates ROI from the execution results. Each step's output flows to the next via JSONPath expressions.

## The Solution

**You just write the campaign design, targeting, execution, and measurement workers. Conductor handles the phase sequencing and data flow between them.**

Four workers handle the campaign lifecycle. design, targeting, execution, and measurement. The design worker generates a campaign ID and assigns channels. The targeting worker builds an audience segment constrained by budget. The execution worker delivers the campaign to the audience. The measurement worker computes ROI and engagement metrics. Conductor enforces the sequence and passes campaign IDs, audience data, and execution IDs between steps automatically.

### What You Write: Workers

DesignWorker creates campaign assets, TargetWorker builds the audience, ExecuteWorker delivers across channels, and MeasureWorker calculates ROI, each handles one phase of the marketing lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **DesignWorker** | `cpa_design` | Creates a campaign with creative assets and channel assignments (email, social, display). |
| **ExecuteWorker** | `cpa_execute` | Launches the campaign by delivering it to the targeted audience across all assigned channels. |
| **MeasureWorker** | `cpa_measure` | Calculates campaign ROI and engagement metrics (impressions, clicks, conversions). |
| **TargetWorker** | `cpa_target` | Builds a targeted audience segment for the campaign within the specified budget. |

### The Workflow

```
cpa_design
 │
 ▼
cpa_target
 │
 ▼
cpa_execute
 │
 ▼
cpa_measure

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
