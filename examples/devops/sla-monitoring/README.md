# Tracking SLA Compliance with Error Budget Calculations

Your payment-api has a 99.95% availability SLO, but nobody checks compliance until the
quarterly business review -- when it is too late to fix a breach. This workflow collects
service level indicators, calculates the remaining error budget, evaluates compliance, and
generates a stakeholder report.

## Workflow

```
service, sloTarget
       |
       v
+--------------------+     +------------------------+     +----------------------------+     +--------------+
| sla_collect_slis   | --> | sla_calculate_budget   | --> | sla_evaluate_compliance    | --> | sla_report   |
+--------------------+     +------------------------+     +----------------------------+     +--------------+
  COLLECT_SLIS-1339         73% budget remaining           SLA compliant, within           report generated
  99.95% availability       21.6 min left this month       error budget                    for stakeholders
  p99 = 180ms
```

## Workers

**CollectSlisWorker** -- Collects SLIs for payment-api: 99.95% availability and p99 latency
of 180ms. Returns `collect_slisId: "COLLECT_SLIS-1339"`.

**CalculateBudgetWorker** -- Computes error budget: 73% remaining, 21.6 minutes left this
month. Returns `calculate_budget: true`.

**EvaluateComplianceWorker** -- Confirms the service is SLA compliant and within error
budget. Returns `evaluate_compliance: true`.

**ReportWorker** -- Generates an SLA report for stakeholders. Returns `report: true`.

## Tests

2 unit tests cover the SLA monitoring pipeline.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
