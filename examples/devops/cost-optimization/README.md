# Cutting 23% Cloud Waste with Automated Cost Optimization

Your AWS bill grew 40% last quarter but traffic only grew 10%. Somewhere in the account
there are idle instances, oversized volumes, and forgotten resources bleeding money. This
workflow collects billing data, analyzes usage to find waste, generates recommendations,
and applies safe savings automatically.

## Workflow

```
account, period
      |
      v
+----------------------+     +--------------------+     +----------------+     +---------------------+
| co_collect_billing   | --> | co_analyze_usage   | --> | co_recommend   | --> | co_apply_savings    |
+----------------------+     +--------------------+     +----------------+     +---------------------+
  COLLECT_BILLING-1349        23% waste found            5 recommendations     8 right-sized, 10
  prod-account-001            idle + oversized           generated             idle terminated
```

## Workers

**CollectBillingWorker** -- Collects billing data for the specified `account` and `period`.
Returns `collect_billingId: "COLLECT_BILLING-1349"`.

**AnalyzeUsageWorker** -- Analyzes the billing data and identifies 23% waste from idle
instances and oversized volumes. Returns `analyze_usage: true`.

**RecommendWorker** -- Generates 5 optimization recommendations based on the usage analysis.
Returns `recommend: true`.

**ApplySavingsWorker** -- Applies the recommendations: right-sizes 8 instances and terminates
10 idle resources. Returns `apply_savings: true`.

## Tests

2 unit tests cover the cost optimization pipeline.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
