# Property Valuation in Java with Conductor : Comparable Sales, Market Analysis, Appraisal, and Report

## The Problem

You need to determine the market value of a property. An accurate valuation requires gathering recent comparable sales in the same neighborhood (same size, age, features), analyzing how the comps compare to the subject property (adjusting for differences in square footage, lot size, upgrades), producing an appraised value based on the analysis, and generating a formal report that lenders and buyers can rely on. Each step depends on the previous one. the analysis needs comps, the appraisal needs the analysis, and the report needs everything.

Without orchestration, property valuations are manual and inconsistent. Appraisers search for comps in multiple MLS systems, perform adjustments on paper or in spreadsheets, and write reports from scratch. When the MLS data source is slow, the entire valuation stalls. When an appraiser is unavailable, nobody knows which step the valuation reached. Lenders waiting on the appraisal have no visibility into progress.

## The Solution

**You just write the comparable sales collection, market analysis, appraisal estimation, and report generation logic. Conductor handles comparable search retries, adjustment calculations, and appraisal audit trails.**

Each valuation step is a simple, independent worker. one collects comparable sales, one performs market analysis with adjustments, one produces the appraisal estimate, one generates the formal report. Conductor takes care of executing them in order, retrying if the MLS data feed times out, and tracking every valuation from comps collection through report delivery.

### What You Write: Workers

Data collection, comparable analysis, adjustment calculation, and appraisal report workers each contribute one layer to determining property value.

| Worker | Task | What It Does |
|---|---|---|
| **CollectCompsWorker** | `pvl_collect_comps` | Gathers recent comparable sales near the subject property. similar size, age, and features |
| **AnalyzeWorker** | `pvl_analyze` | Performs market analysis on comps, adjusting for differences in square footage, lot size, and upgrades |
| **AppraiseWorker** | `pvl_appraise` | Produces the appraisal estimate based on the adjusted comp analysis |
| **ReportWorker** | `pvl_report` | Generates the formal valuation report with comps, adjustments, and final estimated value |

Workers implement property transaction steps. listing, inspection, escrow, closing, with realistic outputs.

### The Workflow

```
pvl_collect_comps
 │
 ▼
pvl_analyze
 │
 ▼
pvl_appraise
 │
 ▼
pvl_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
