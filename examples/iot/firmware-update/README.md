# Firmware Update

Orchestrates firmware update through a multi-stage Conductor workflow.

**Input:** `deviceId`, `currentVersion`, `targetVersion` | **Timeout:** 60s

## Pipeline

```
fw_check_version
    │
fw_download
    │
fw_validate
    │
fw_deploy
    │
fw_verify
```

## Workers

**CheckVersionWorker** (`fw_check_version`)

Reads `updateAvailable`. Outputs `updateAvailable`, `downloadUrl`, `checksum`, `releaseNotes`, `sizeMb`.

**DeployWorker** (`fw_deploy`)

Reads `deploymentId`. Outputs `deploymentId`, `deployedAt`, `rebootRequired`, `rebootScheduled`.

**DownloadWorker** (`fw_download`)

Reads `firmwarePath`. Outputs `firmwarePath`, `downloadedSizeMb`, `downloadTimeMs`.

**ValidateWorker** (`fw_validate`)

Reads `validated`. Outputs `validated`, `checksumMatch`, `signatureValid`, `compatibilityCheck`.

**VerifyWorker** (`fw_verify`)

Reads `targetVersion`, `verified`. Outputs `verified`, `runningVersion`, `bootTime`, `selfTestPassed`, `rollbackAvailable`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
