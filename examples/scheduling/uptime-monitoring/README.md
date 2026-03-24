# Uptime Monitoring in Java Using Conductor : Endpoint Checks, Result Logging, SLA Calculation, and Reporting

## The Problem

You need to monitor whether your endpoints are up and meeting SLA commitments. Each endpoint must be checked for availability (is it returning the expected status code?), results must be logged for historical analysis, SLA compliance must be calculated (are you meeting your 99.9% uptime guarantee?), and reports must be generated for stakeholders and customers.

Without orchestration, uptime monitoring is a simple ping script that checks URLs and sends alerts. Historical data is not preserved, SLA calculations are done manually in spreadsheets, and uptime reports are created ad hoc when a customer asks. There's no automated pipeline from check to report.

## The Solution

**You just write the availability checks and SLA compliance calculations. Conductor handles the check-log-calculate-report pipeline, retries when endpoints are unreachable or metric stores are slow, and a full history of every monitoring cycle with response times and SLA standings.**

Each monitoring concern is an independent worker. endpoint checking, result logging, SLA calculation, and report generation. Conductor runs them in sequence: check the endpoint, log the result, calculate SLA compliance, then generate the report. Every monitoring run is tracked with the check result, response time, and SLA standing.

### What You Write: Workers

CheckEndpointWorker probes each endpoint for availability and response time, LogResultWorker records the outcome for trending, CalculateSlaWorker computes uptime percentages against your SLA target, and UmReportWorker generates the stakeholder-facing report.

| Worker | Task | What It Does |
|---|---|---|
| **CalculateSlaWorker** | `um_calculate_sla` | Calculates current SLA percentage from total checks, determining whether the SLA target is met |
| **CheckEndpointWorker** | `um_check_endpoint` | Checks an endpoint's availability, returning HTTP status, response time in milliseconds, and up/down status |
| **LogResultWorker** | `um_log_result` | Logs the endpoint check result (endpoint, status) for historical trending |
| **UmReportWorker** | `um_report` | Generates an uptime/SLA report summarizing availability metrics for stakeholders |

the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
um_check_endpoint
 │
 ▼
um_log_result
 │
 ▼
um_calculate_sla
 │
 ▼
um_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
