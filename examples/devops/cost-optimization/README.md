# Cloud Cost Optimization in Java with Conductor : Billing Collection, Usage Analysis, Savings Recommendations, and Auto-Apply

Orchestrates cloud cost optimization using [Conductor](https://github.com/conductor-oss/conductor). This workflow collects billing data for a specified account and period, analyzes resource utilization to identify waste (idle instances, over-provisioned databases, unused EBS volumes), generates savings recommendations with estimated dollar impact, and auto-applies safe optimizations like purchasing reserved instances or deleting orphaned snapshots.

## The Cloud Bill Problem

Your AWS bill jumped 40% last month. Somewhere in your account, there are EC2 instances running at 5% CPU, over-provisioned RDS databases with 2TB allocated but only 50GB used, and EBS snapshots from instances deleted six months ago. Finding these wastes requires collecting billing data, cross-referencing it with utilization metrics, computing how much each optimization would save, and then actually applying the changes. Rightsizing instances, deleting orphaned resources, converting on-demand to reserved capacity. Missing even one step means money leaking out every hour.

Without orchestration, you'd write a cost analysis script that pulls billing data, scans for waste, and maybe sends an email with recommendations. But nobody acts on the email. The recommendations go stale because utilization patterns change daily. There's no record of which optimizations were applied, how much they actually saved, or whether the changes caused performance issues.

## The Solution

**You write the billing analysis and optimization logic. Conductor handles the collection-to-savings pipeline and tracks every dollar saved.**

Each stage of the cost optimization pipeline is a simple, independent worker. The billing collector pulls spend data for the specified account and period from the cloud provider's cost API. The usage analyzer cross-references billing with utilization metrics to identify waste: instances running below 10% CPU, databases with unused storage, orphaned load balancers with no targets. The recommender generates prioritized savings opportunities with estimated monthly dollar impact for each. The savings applier executes safe optimizations automatically, deleting orphaned snapshots, rightsizing instances, purchasing reserved capacity, and reports what was applied and the projected savings. Conductor executes them in strict sequence, retries if the billing API is rate-limited, and tracks spend amounts and savings amounts at every stage.

### What You Write: Workers

Four workers run the cost optimization cycle. Collecting billing data, analyzing resource utilization, generating savings recommendations, and auto-applying safe optimizations.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeUsageWorker** | `co_analyze_usage` | Cross-references billing with utilization to find waste: idle instances, oversized volumes, orphaned resources |
| **ApplySavingsWorker** | `co_apply_savings` | Executes safe optimizations: right-sizes instances, terminates idle resources, purchases reserved capacity |
| **CollectBillingWorker** | `co_collect_billing` | Pulls billing and spend data for the specified account and period from the cloud cost API |
| **RecommendWorker** | `co_recommend` | Generates prioritized savings recommendations with estimated dollar impact per optimization |

the workflow and rollback logic stay the same.

### The Workflow

```
co_collect_billing
 │
 ▼
co_analyze_usage
 │
 ▼
co_recommend
 │
 ▼
co_apply_savings

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
