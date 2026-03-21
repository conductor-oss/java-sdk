# Population Health Management in Java Using Conductor : Data Aggregation, Risk Stratification, Care Gap Identification, and Intervention

## The Problem

You need to manage population health for a patient cohort with a specific condition during a reporting period. Clinical and claims data must be aggregated from multiple sources. EHRs, claims feeds, pharmacy records, and lab results. Members must be risk-stratified into tiers (low, moderate, high, rising risk) based on HCC scores, utilization patterns, and social determinants. Care gaps must be identified. patients overdue for A1C tests, mammograms, colonoscopies, or flu vaccines per HEDIS/STAR quality measures. Targeted interventions must then be triggered, outreach calls, care coordinator assignments, appointment scheduling, or pharmacy consultations. Each step depends on the previous one, you cannot identify care gaps without risk stratification, and you cannot intervene without knowing which gaps exist.

Without orchestration, you'd build a monolithic population health analytics engine that queries your data warehouse, runs the risk model, scans for gaps, and generates outreach lists. If the claims data feed is delayed, the entire pipeline stalls. If the system crashes after risk stratification but before gap identification, you have risk scores but no actionable gaps. CMS quality programs (STARS, MIPS, MSSP) require documentation of every population health activity for performance reporting.

## The Solution

**You just write the population health workers. Data aggregation, risk stratification, care gap identification, and intervention triggering. Conductor handles pipeline ordering, automatic retries when a data feed is delayed, and complete documentation for CMS quality program reporting.**

Each stage of the population health pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of aggregating data before stratification, identifying gaps only after risk levels are assigned, triggering interventions only for identified gaps, and maintaining a complete audit trail for CMS quality reporting. ### What You Write: Workers

Four workers drive the population health pipeline: AggregateDataWorker pulls clinical and claims data, StratifyRiskWorker assigns risk tiers, IdentifyGapsWorker finds missed screenings and overdue interventions, and InterveneWorker triggers targeted outreach.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateDataWorker** | `pop_aggregate_data` | Aggregates clinical, claims, pharmacy, and lab data for the specified cohort and reporting period |
| **StratifyRiskWorker** | `pop_stratify_risk` | Assigns risk tiers (low, moderate, high, rising risk) based on HCC scores, utilization, and SDOH factors |
| **IdentifyGapsWorker** | `pop_identify_gaps` | Scans for care gaps. overdue screenings, missing labs, uncontrolled chronic conditions per HEDIS/STARS measures |
| **InterveneWorker** | `pop_intervene` | Triggers targeted interventions. outreach calls, care coordinator assignments, appointment scheduling |

the workflow and compliance logic stay the same.

### The Workflow

```
pop_aggregate_data
 │
 ▼
pop_stratify_risk
 │
 ▼
pop_identify_gaps
 │
 ▼
pop_intervene

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
