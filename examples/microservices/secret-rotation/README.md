# Rotating Secrets with Vault Storage and Service Update

Secrets expire, but rotating them manually risks forgetting to update a service, causing
outages when the old secret is revoked. This workflow generates a new secret, stores it in
Vault at the correct path, updates all target services (no restart required), and verifies
the old secret is revoked.

## Workflow

```
secretName, targetServices
          |
          v
+----------------------+     +---------------------+     +-----------------------+     +-----------------------+
| sr_generate_secret   | --> | sr_store_secret     | --> | sr_update_services    | --> | sr_verify_rotation    |
+----------------------+     +---------------------+     +-----------------------+     +-----------------------+
  secretId:                    vaultPath:                  updatedServices list          oldSecretRevoked: true
  sec-rot-20240301-001         secret/data/{name}         updatedCount: N               allServicesUsing:
  expiresAt: 2024-06-01       version: 3                  restartRequired: false        new secretId
```

## Workers

**GenerateSecretWorker** -- Generates a new secret. Returns
`secretId: "sec-rot-20240301-001"`, `algorithm`, `expiresAt: "2024-06-01T00:00:00Z"`.

**StoreSecretWorker** -- Stores the secret in Vault. Returns
`vaultPath: "secret/data/{secretName}"`, `version: 3`, `stored: true`.

**UpdateServicesWorker** -- Updates target services with the new secret. Returns
`updatedServices`, `updatedCount`, `restartRequired: false`.

**VerifyRotationWorker** -- Confirms rotation: `allServicesUsing` the new secretId,
`oldSecretRevoked: true`, `status: "success"`.

## Tests

12 unit tests cover secret generation, vault storage, service updates, and rotation
verification.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
