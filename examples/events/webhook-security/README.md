# Webhook Security in Java Using Conductor

Webhook security workflow that computes an HMAC signature, verifies it against the provided signature, and routes to process or reject the webhook via a SWITCH task.

## The Problem

You need to verify the authenticity of incoming webhooks using HMAC signatures. The workflow computes an HMAC hash of the payload using the shared secret, compares it to the signature provided by the sender, and routes to processing (if valid) or rejection (if forged). Without signature verification, an attacker can send forged webhooks that trigger unauthorized actions in your system.

Without orchestration, you'd embed HMAC verification in middleware, manually computing hashes, comparing signatures with timing-safe equality, handling missing or malformed signatures, and logging every verification result for security audit. hoping the verification is never accidentally bypassed by a code change.

## The Solution

**You just write the HMAC-compute, signature-verify, process, and reject workers. Conductor handles SWITCH-based accept/reject routing, guaranteed verification before processing, and a security audit trail for every webhook.**

Each security concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of computing the HMAC, verifying the signature, routing via a SWITCH task to process (valid) or reject (invalid), and tracking every webhook's verification result for security audit.

### What You Write: Workers

Four workers verify webhook authenticity: ComputeHmacWorker generates the expected signature, VerifySignatureWorker compares it to the sender's value, ProcessWebhookWorker handles verified payloads, and RejectWebhookWorker blocks forged requests.

| Worker | Task | What It Does |
|---|---|---|
| **ComputeHmacWorker** | `ws_compute_hmac` | Computes an HMAC signature for a webhook payload. |
| **ProcessWebhookWorker** | `ws_process_webhook` | Processes a verified webhook payload. |
| **RejectWebhookWorker** | `ws_reject_webhook` | Rejects a webhook with an invalid signature. |
| **VerifySignatureWorker** | `ws_verify_signature` | Verifies a webhook signature by comparing expected vs provided values. |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources. the workflow and routing logic stay the same.

### The Workflow

```
ws_compute_hmac
 │
 ▼
ws_verify_signature
 │
 ▼
SWITCH (validity_switch_ref)
 ├── valid: ws_process_webhook
 ├── invalid: ws_reject_webhook

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
