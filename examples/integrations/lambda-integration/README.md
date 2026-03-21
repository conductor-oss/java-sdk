# Lambda Integration in Java Using Conductor

## Invoking Lambda Functions with Proper Orchestration

Invoking a Lambda function is more than a single API call. The payload needs to be prepared and validated, the function needs to be invoked with the right qualifier (version/alias), the response needs to be parsed and processed for downstream use, and the execution needs to be logged for auditing. Each step depends on the previous one. you cannot invoke without a payload, and you cannot process without a response.

Without orchestration, you would chain AWS SDK calls manually, manage payloads and response objects between steps, and build custom logging. Conductor sequences the pipeline and passes function names, payloads, and execution results between workers automatically.

## The Solution

**You just write the Lambda orchestration workers. Payload preparation, function invocation, response processing, and execution logging. Conductor handles payload-to-log sequencing, Lambda invocation retries, and execution metadata routing between stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers orchestrate Lambda invocations: PreparePayloadWorker validates and formats input, InvokeLambdaWorker calls the function, ProcessResponseWorker parses the output, and LogResultWorker records the execution for auditing.

| Worker | Task | What It Does |
|---|---|---|
| **PreparePayloadWorker** | `lam_prepare_payload` | Prepares the Lambda invocation payload. validates input data, formats the JSON payload, and determines the function name and qualifier (version/alias) |
| **InvokeLambdaWorker** | `lam_invoke` | Invokes the AWS Lambda function. calls the specified function with the prepared payload and returns the response body and execution duration |
| **ProcessResponseWorker** | `lam_process_response` | Processes the Lambda response. parses the response body, extracts relevant fields, and prepares the output for downstream use |
| **LogResultWorker** | `lam_log_result` | Logs the execution result. records the function name, duration, status, and processed output for auditing |

the workflow orchestration and error handling stay the same.

### The Workflow

```
lam_prepare_payload
 │
 ▼
lam_invoke
 │
 ▼
lam_process_response
 │
 ▼
lam_log_result

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
