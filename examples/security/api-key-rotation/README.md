# Implementing API Key Rotation in Java with Conductor : Generate, Dual-Active Period, Consumer Migration, and Revocation

## The Problem

You need to rotate API keys without downtime. Simply replacing the old key with a new one breaks every consumer that's still using the old key. Safe rotation requires generating a new key, keeping both old and new active simultaneously (dual-active period), migrating each consumer to the new key, and only revoking the old key once all consumers have switched. If revocation happens before migration, services go down.

Without orchestration, API key rotation is either avoided entirely (keys never rotate, increasing breach risk) or done recklessly (old key revoked before consumers migrate, causing outages). There's no tracking of which consumers have migrated, and the dual-active period is managed manually.

## The Solution

**You just write the key generation and consumer migration logic. Conductor handles strict ordering so revocation never happens before migration, retries if a consumer update fails, and tracking of which consumers are still on the old key.**

Each rotation step is an independent worker. key generation, dual-active activation, consumer migration, and old key revocation. Conductor runs them in strict sequence: generate the new key, activate dual-active mode, migrate consumers, then revoke the old key. Every rotation is tracked with consumer migration status, you can see exactly which consumers are still on the old key.

### What You Write: Workers

Four workers execute zero-downtime rotation: GenerateNewWorker creates a fresh API key, DualActiveWorker keeps both old and new keys valid simultaneously, MigrateConsumersWorker switches each consumer to the new key, and RevokeOldWorker retires the old key once all consumers have migrated.

| Worker | Task | What It Does |
|---|---|---|
| **DualActiveWorker** | `akr_dual_active` | Activates dual-key mode so both old and new keys are valid during the transition window |
| **GenerateNewWorker** | `akr_generate_new` | Generates a new API key for the specified service |
| **MigrateConsumersWorker** | `akr_migrate_consumers` | Migrates all consumers to the new key and confirms each has switched |
| **RevokeOldWorker** | `akr_revoke_old` | Revokes the old API key after all consumers have been migrated to the new one |

the workflow logic stays the same.

### The Workflow

```
Input -> DualActiveWorker -> GenerateNewWorker -> MigrateConsumersWorker -> RevokeOldWorker -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
