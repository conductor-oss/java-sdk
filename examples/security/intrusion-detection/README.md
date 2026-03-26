# Intrusion Detection

IP address `198.51.100.42` triggers repeated authentication failures. The system must analyze 47 suspicious events, correlate the IP against known threat intelligence feeds, assess the severity as critical (active brute-force from known malicious IP), and respond by blocking the IP and notifying the security team.

## Workflow

```
id_analyze_events ──> id_correlate_threats ──> id_assess_severity ──> id_respond
```

Workflow `intrusion_detection_workflow` accepts `sourceIp` and `eventType`. Times out after `120` seconds.

## Workers

**AnalyzeEventsWorker** (`id_analyze_events`) -- reports `"198.51.100.42: repeated-auth-failure -- 47 suspicious events"`. Returns `analyze_eventsId` = `"ANALYZE_EVENTS-1359"`.

**CorrelateThreatsWorker** (`id_correlate_threats`) -- matches against threat feeds. Reports `"IP matched known threat intelligence feed"`. Returns `correlate_threats` = `true`.

**AssessSeverityWorker** (`id_assess_severity`) -- evaluates threat level. Reports `"Critical threat: active brute-force from known malicious IP"`. Returns `assess_severity` = `true`.

**RespondWorker** (`id_respond`) -- executes the response. Reports `"Blocked IP, notified security team"`. Returns `respond` = `true`.

## Workflow Output

The workflow produces `analyze_eventsResult`, `respondResult` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `intrusion_detection_workflow` defines 4 tasks with input parameters `sourceIp`, `eventType` and a timeout of `120` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify the end-to-end intrusion detection pipeline from event analysis through response.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
