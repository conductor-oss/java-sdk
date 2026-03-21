# Customer Churn in Java Using Conductor

## Why Churn Prevention Needs Orchestration

Retaining at-risk customers requires a time-sensitive pipeline from detection through outcome tracking. You detect churn risk by analyzing usage trends. declining call minutes, reduced data consumption, or increased complaint frequency signal a customer is likely to leave. You analyze the reasons behind the risk, is it pricing, network quality in their area, a recent bad support experience, or a competitor offer? You create a personalized retention offer based on the identified reasons and the customer's account age, a discount, a plan upgrade, bonus data, or a device credit. You deliver the offer via the most effective channel for that customer. Finally, you track whether the offer was accepted and the customer was retained.

If the offer is created but delivery fails, a perfectly good retention offer never reaches the customer and they churn. If detection flags a customer but reason analysis stalls, the retention team misses the intervention window. Without orchestration, you'd build a batch churn-scoring script that dumps results into a spreadsheet for manual follow-up. making it impossible to personalize offers at scale, track which offers actually prevent churn, or close the loop between the churn model and retention outcomes.

## The Solution

**You just write the churn detection, reason analysis, retention offer creation, offer delivery, and outcome tracking logic. Conductor handles scoring retries, offer routing, and retention campaign audit trails.**

Each worker handles one telecom operation. Conductor manages the provisioning pipeline, activation sequencing, billing triggers, and service state tracking.

### What You Write: Workers

Risk scoring, retention offer generation, outreach execution, and outcome tracking workers each tackle one stage of reducing customer attrition.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeReasonsWorker** | `ccn_analyze_reasons` | Analyzes the reasons behind the churn risk. pricing, coverage, service quality, or competitor offers. |
| **CreateOfferWorker** | `ccn_create_offer` | Creates a personalized retention offer based on the identified churn reasons and account tenure. |
| **DeliverWorker** | `ccn_deliver` | Delivers the retention offer to the customer via their preferred channel (SMS, email, in-app, call center). |
| **DetectRiskWorker** | `ccn_detect_risk` | Detects churn risk by scoring the customer based on usage trend decline and behavioral signals. |
| **TrackWorker** | `ccn_track` | Tracks whether the customer accepted the offer and was retained or still churned. |

### The Workflow

```
ccn_detect_risk
 │
 ▼
ccn_analyze_reasons
 │
 ▼
ccn_create_offer
 │
 ▼
ccn_deliver
 │
 ▼
ccn_track

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
