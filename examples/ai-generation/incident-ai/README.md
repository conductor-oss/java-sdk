# Incident AI: Detect, Diagnose, Fix, and Verify

A production service starts returning errors on its checkout and payment endpoints during peak traffic. The on-call engineer needs to detect the incident, diagnose the root cause, propose and execute a fix, and verify that the service has recovered -- all under time pressure. Manual triage through dashboards and runbooks is slow and error-prone, especially at 3 AM.

This workflow automates the full incident response lifecycle: detection, diagnosis, fix suggestion, fix execution, and verification.

## Pipeline Architecture

```
alertId, serviceName, severity
         |
         v
  iai_detect             (incidentId="INC-2024-650", incidentDetails map)
         |
         v
  iai_diagnose           (diagnosis with root cause and confidence)
         |
         v
  iai_suggest_fix        (suggestedFix with action and rationale)
         |
         v
  iai_execute_fix        (fixApplied=true, action="gateway_failover")
         |
         v
  iai_verify             (verified=true, errorRate=0.001, status="healthy")
```

## Worker: Detect (`iai_detect`)

Identifies the incident from the alert. Returns `incidentId: "INC-2024-650"` and an `incidentDetails` map with affected endpoints (`["/api/checkout", "/api/payment"]`), error patterns, and timing information. The incident ID follows the `INC-{year}-{sequence}` convention.

## Worker: Diagnose (`iai_diagnose`)

Analyzes the incident details -- endpoint count, error patterns, and affected services -- to determine the root cause. Returns a `diagnosis` map containing the identified root cause, contributing factors, and a confidence score. The diagnosis drives the fix suggestion.

## Worker: SuggestFix (`iai_suggest_fix`)

Proposes a remediation based on the diagnosis. Returns a `suggestedFix` map with the recommended action, step-by-step execution plan, expected impact, and risk assessment. The fix is specific to the diagnosed root cause.

## Worker: ExecuteFix (`iai_execute_fix`)

Applies the suggested fix. Returns `fixApplied: true` and `action: "gateway_failover"` -- indicating that the fix involved failing over to a backup gateway to restore service on the affected checkout and payment endpoints.

## Worker: Verify (`iai_verify`)

Confirms the fix resolved the incident. Returns `verified: true`, `errorRate: 0.001` (down from the elevated rate during the incident), and `status: "healthy"`. The low error rate confirms the service has fully recovered.

## Tests

5 tests cover detection, diagnosis, fix suggestion, fix execution, and verification.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
