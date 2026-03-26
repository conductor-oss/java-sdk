# Webhook Security

A platform receives webhook requests from external services. Each request needs HMAC signature verification against a shared secret, timestamp validation to prevent replay attacks, IP allowlist checking, and payload integrity verification before the event is accepted for processing.

## Pipeline

```
[ws_compute_hmac]
     |
     v
[ws_verify_signature]
     |
     v
     <SWITCH>
       |-- valid -> [ws_process_webhook]
       |-- invalid -> [ws_reject_webhook]
```

**Workflow inputs:** `payload`, `providedSignature`

## Workers

**ComputeHmacWorker** (task: `ws_compute_hmac`)

Computes an HMAC signature for a webhook payload.

- Sets `algorithm` = `"sha256"`
- Reads `payload`, `secret`. Writes `computedSignature`, `algorithm`

**ProcessWebhookWorker** (task: `ws_process_webhook`)

Processes a verified webhook payload.

- Reads `payload`. Writes `processed`

**RejectWebhookWorker** (task: `ws_reject_webhook`)

Rejects a webhook with an invalid signature.

- Reads `reason`, `provided`. Writes `rejected`, `reason`

**VerifySignatureWorker** (task: `ws_verify_signature`)

Verifies a webhook signature by comparing expected vs provided values.

- Reads `expected`, `provided`. Writes `result`, `match`

---

**34 tests** | Workflow: `webhook_security_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
