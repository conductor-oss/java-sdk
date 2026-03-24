# Secret Rotation in Java with Conductor

Rotate secrets across services securely.

## The Problem

Secrets (API keys, database passwords, encryption keys) must be rotated periodically to limit the blast radius of a leak. Rotation involves generating a new secret, storing it in a vault, updating every dependent service to use the new secret, and verifying that all services have switched over and the old secret is revoked.

Without orchestration, secret rotation is a manual runbook where an engineer generates a secret, updates Vault, then SSH-es into each service to restart it. Missing a service means it still uses the old secret, and there is no verification step.

## The Solution

**You just write the secret-generation, vault-storage, service-update, and rotation-verification workers. Conductor handles ordered rotation steps, crash-safe resume between vault storage and service update, and a complete rotation audit.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers automate the rotation lifecycle: GenerateSecretWorker produces a new cryptographic key, StoreSecretWorker writes it to a vault, UpdateServicesWorker rolls the change across dependent services, and VerifyRotationWorker confirms the old secret is revoked.

| Worker | Task | What It Does |
|---|---|---|
| **GenerateSecretWorker** | `sr_generate_secret` | Generates a new cryptographic secret using the specified algorithm (e.g., AES-256) and returns its ID and expiry. |
| **StoreSecretWorker** | `sr_store_secret` | Stores the generated secret in a vault (e.g., HashiCorp Vault) at a versioned path. |
| **UpdateServicesWorker** | `sr_update_services` | Updates all target services to reference the new secret, reporting which services were updated. |
| **VerifyRotationWorker** | `sr_verify_rotation` | Verifies that all services are using the new secret and that the old secret has been revoked. |

the workflow coordination stays the same.

### The Workflow

```
sr_generate_secret
 │
 ▼
sr_store_secret
 │
 ▼
sr_update_services
 │
 ▼
sr_verify_rotation

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
