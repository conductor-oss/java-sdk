# Debugging incidents

**Audience:** on-call engineers and workflow owners.  
**Works with:** OSS and Orkes.

Start with an execution ID, not a code guess. Inspect the workflow status, failed task, reason for incompletion, retry count, input/output shape, and worker logs for the same task ID.

## Fast triage

| Symptom | Likely cause | First check |
|---|---|---|
| Task stays `SCHEDULED` | No matching worker, wrong domain, or no task definition | worker task name/domain and queue age |
| Task times out | worker crash, blocked dependency, or timeout too low | worker logs, response timeout, downstream latency |
| Task repeatedly fails | transient failure misclassified or non-idempotent side effect | reason, retry policy, idempotency key |
| Agent waits forever | approval/tool response was never sent | agent status and pending HITL task |
| `401`/`403` | credentials or tenant permissions | endpoint and injected credential identity |

## Gather safe evidence

Capture IDs, timestamps, status, reason, retry count, and redacted configuration. Do not paste auth headers, raw secrets, or customer payloads into tickets. If a definition changed recently, compare the execution's workflow version before rolling back.

Next: [reliability](reliability.md), [connection and authentication](connection-authentication.md), and [agent client reference](agents/reference/client.md).
