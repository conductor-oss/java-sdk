# A/B Testing Pipeline in Java Using Conductor : Variant Definition, User Assignment, Metric Collection, and Statistical Analysis

## Why A/B Test Pipelines Need Orchestration

Running a rigorous A/B test involves a strict sequence where each stage depends on the previous one. You define the variants (control vs. treatment) and traffic split percentages. You assign users to groups using deterministic hashing so the same user always sees the same variant. You collect behavioral metrics. clicks, impressions, conversion rates, for each group over the experiment window. You run statistical analysis to compute p-values, measure uplift, and determine whether results are statistically significant at your target confidence level. Finally, you decide a winner and generate a rollout recommendation.

If user assignment fails midway, you need to know which users were already assigned to avoid reassignment that would corrupt your experiment. If metric collection is incomplete, the statistical analysis will produce unreliable results. Without orchestration, you'd build a monolithic experimentation framework that mixes user bucketing, event aggregation, and statistical computation. making it impossible to swap your randomization strategy, test significance calculations independently, or audit which users were in which group.

## How This Workflow Solves It

**You just write the experimentation workers. Variant definition, user bucketing, metric collection, statistical analysis, and winner selection. Conductor handles experiment sequencing, metric collection retries, and a complete audit trail of every configuration and group assignment.**

Each experiment stage is an independent worker. define variants, assign users, collect data, analyze results, decide winner. Conductor sequences them, passes variant definitions through user assignment into data collection and analysis, retries if a metric query times out, and maintains a complete audit trail of every experiment configuration, group assignment, and statistical result.

### What You Write: Workers

Five workers drive the experiment lifecycle: DefineVariantsWorker sets up control and treatment groups, AssignUsersWorker buckets participants via hashing, CollectDataWorker gathers engagement metrics, AnalyzeResultsWorker runs statistical tests, and DecideWinnerWorker picks the rollout winner.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeResultsWorker** | `abt_analyze_results` | Analyzes results |
| **AssignUsersWorker** | `abt_assign_users` | Assigns users |
| **CollectDataWorker** | `abt_collect_data` | Collects data |
| **DecideWinnerWorker** | `abt_decide_winner` | Handles decide winner |
| **DefineVariantsWorker** | `abt_define_variants` | Defines variants |

Workers implement media processing stages. transcoding, thumbnail generation, metadata extraction, with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
abt_define_variants
 │
 ▼
abt_assign_users
 │
 ▼
abt_collect_data
 │
 ▼
abt_analyze_results
 │
 ▼
abt_decide_winner

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
