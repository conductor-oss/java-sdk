# Email Approval

Email-based approval workflow with click links

## Pipeline

```
ea_prepare
    │
ea_send_email
    │
email_response [WAIT]
    │
ea_process_decision
```

## Workers

**PrepareWorker** (`ea_prepare`): Worker for ea_prepare — prepares the approval request.

Outputs `ready`.

**ProcessDecisionWorker** (`ea_process_decision`): Worker for ea_process_decision — processes the approval decision.

Reads `decision`. Outputs `processed`.

**SendEmailWorker** (`ea_send_email`): Worker for ea_send_email — performs sending an approval email with click links.

- `BASE_URL` = `"https://example.com/approval"`

```java
String workflowId = task.getWorkflowInstanceId() != null
```

Outputs `sent`, `approveUrl`, `rejectUrl`.

## Tests

**19 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
