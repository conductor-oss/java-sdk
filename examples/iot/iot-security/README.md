# Iot Security

Orchestrates iot security through a multi-stage Conductor workflow.

**Input:** `networkId`, `scanDepth` | **Timeout:** 60s

## Pipeline

```
ios_scan_devices
    │
ios_detect_vulnerabilities
    │
ios_patch
    │
ios_verify
```

## Workers

**DetectVulnerabilitiesWorker** (`ios_detect_vulnerabilities`)

Outputs `vulnerabilities`, `vulnCount`, `affectedDevices`.

**PatchWorker** (`ios_patch`)

Outputs `patchedDevices`, `patchCount`.

**ScanDevicesWorker** (`ios_scan_devices`)

Outputs `deviceCount`.

**VerifyWorker** (`ios_verify`)

Outputs `allClear`, `verifiedCount`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
