# DDoS Mitigation

An API gateway detects traffic at 500% above baseline. The system must detect the anomaly, classify it as a Layer 7 HTTP flood from a botnet, apply mitigation (rate limiting, challenge pages, IP blocking), and verify that legitimate traffic is flowing normally again.

## Workflow

```
ddos_detect ──> ddos_classify ──> ddos_mitigate ──> ddos_verify_service
```

Workflow `ddos_mitigation_workflow` accepts `targetService` and `trafficAnomaly`. Times out after `300` seconds.

## Workers

**DetectWorker** (`ddos_detect`) -- reports `"api-gateway: traffic 500% above baseline"`. Returns `detectId` = `"DETECT-1353"`.

**ClassifyWorker** (`ddos_classify`) -- classifies the attack type. Reports `"Layer 7 HTTP flood attack from botnet"`. Returns `classify` = `true`.

**MitigateWorker** (`ddos_mitigate`) -- applies countermeasures. Reports `"Rate limiting enabled, challenge page activated, 1,200 IPs blocked"`. Returns `mitigate` = `true`.

**VerifyServiceWorker** (`ddos_verify_service`) -- confirms service recovery. Reports `"Service restored: latency normal, legitimate traffic flowing"`. Returns `verify_service` = `true`.

## Workflow Output

The workflow produces `detectResult`, `verify_serviceResult` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `ddos_mitigation_workflow` defines 4 tasks with input parameters `targetService`, `trafficAnomaly` and a timeout of `300` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify the end-to-end DDoS detection, mitigation, and service recovery pipeline.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
