# Webhook Trigger

An automation platform lets users configure workflows that fire when a webhook URL receives a POST request. Each incoming request needs authentication, payload extraction, workflow lookup by webhook ID, and workflow execution with the payload as input.

## Pipeline

```
[wt_process_event]
     |
     v
[wt_validate_payload]
     |
     v
[wt_transform_data]
     |
     v
[wt_store_result]
```

**Workflow inputs:** `eventType`, `payload`, `source`, `timestamp`

## Workers

**ProcessEventWorker** (task: `wt_process_event`)

Processes an incoming webhook event by extracting and parsing the payload into structured data with a known schema.

- Reads `eventType`. Writes `eventType`, `parsedData`, `schema`, `receivedAt`

**StoreResultWorker** (task: `wt_store_result`)

Stores the transformed data into the target destination and returns a confirmation record with the storage details.

- Reads `transformedData`, `destination`. Writes `recordId`, `storedAt`, `destination`

**TransformDataWorker** (task: `wt_transform_data`)

Transforms validated data into a canonical format suitable for storage, normalizing field names and structure.

- Reads `validatedData`, `targetFormat`. Writes `transformedData`, `destination`

**ValidatePayloadWorker** (task: `wt_validate_payload`)

Validates the parsed event payload against the expected schema, performing structural and value checks.

- Reads `eventType`, `parsedData`, `schema`. Writes `valid`, `checks`, `validatedData`, `targetFormat`

---

**36 tests** | Workflow: `webhook_trigger_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
