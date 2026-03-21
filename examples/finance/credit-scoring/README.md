# Credit Scoring in Java with Conductor

Credit scoring: collect data, calculate factors, compute score, classify applicant. ## The Problem

You need to compute a credit score for a loan applicant. This requires collecting financial data (credit history, income, debt), calculating individual scoring factors (payment history, utilization ratio, length of credit, new inquiries), computing a composite score, and classifying the applicant into a risk tier (excellent, good, fair, poor). Each scoring factor depends on the collected data, and the classification depends on the composite score.

Without orchestration, you'd build a single scoring engine that queries credit bureaus, computes factors inline, weights them, and classifies. manually handling bureau API timeouts, caching expensive credit pulls, and logging every factor to explain scores to regulators and applicants who dispute their rating.

## The Solution

**You just write the scoring workers. Financial data collection, factor calculation, composite scoring, and risk classification. Conductor handles pipeline ordering, automatic retries when a credit bureau is slow, and complete factor-level logging for Fair Lending compliance.**

Each scoring concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (collect data, calculate factors, compute score, classify), retrying if a credit bureau is slow, tracking every scoring decision with full factor breakdown for Fair Lending compliance, and resuming from the last step if the process crashes. ### What You Write: Workers

Four workers form the scoring pipeline: CollectDataWorker pulls credit history, CalculateFactorsWorker computes weighted scoring components, ScoreWorker produces the composite score, and ClassifyWorker assigns the risk tier.

| Worker | Task | What It Does |
|---|---|---|
| **CalculateFactorsWorker** | `csc_calculate_factors` | Calculates weighted credit score factors from credit history. |
| **ClassifyWorker** | `csc_classify` | Classifies an applicant based on credit score. |
| **CollectDataWorker** | `csc_collect_data` | Collects credit history data for an applicant. |
| **ScoreWorker** | `csc_score` | Computes a weighted credit score from factor data. |

Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

### The Workflow

```
csc_collect_data
 │
 ▼
csc_calculate_factors
 │
 ▼
csc_score
 │
 ▼
csc_classify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
