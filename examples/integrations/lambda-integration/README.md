# Lambda Integration

Orchestrates lambda integration through a multi-stage Conductor workflow.

**Input:** `functionName`, `inputData`, `qualifier` | **Timeout:** 60s

## Pipeline

```
lam_prepare_payload
    │
lam_invoke
    │
lam_process_response
    │
lam_log_result
```

## Workers

**InvokeLambdaWorker** (`lam_invoke`): Invokes an AWS Lambda function.

Reads `functionName`, `qualifier`. Outputs `statusCode`, `responsePayload`, `duration`, `requestId`, `billedDuration`.

**LogResultWorker** (`lam_log_result`): Logs a Lambda execution result.

Reads `duration`, `executionResult`, `functionName`. Outputs `logged`, `logGroup`.

**PreparePayloadWorker** (`lam_prepare_payload`): Prepares a Lambda invocation payload.

Reads `functionName`, `inputData`. Outputs `payload`.

**ProcessResponseWorker** (`lam_process_response`): Processes a Lambda response.

```java
boolean success = Integer.valueOf(200).equals(statusCode);
```

Reads `responsePayload`, `statusCode`. Outputs `result`, `success`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
