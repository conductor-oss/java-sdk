# Orchestrating a Real CI/CD Pipeline with ProcessBuilder, Security Scanning, and Deployment Validation

A developer pushes to `main`. The pipeline needs to clone the repo, run unit tests, scan for hardcoded secrets, perform integration checks, deploy to staging, and promote to production -- and the testing stages should run in parallel to cut wall-clock time in half. If a hardcoded API key is found, production deployment must be blocked before it happens.

This example builds a CI/CD pipeline where every stage executes real system commands via `ProcessBuilder`, scans real source files with regex-based vulnerability detection, and creates actual deployment artifacts on disk.

## Pipeline Architecture

```
cicd_build  (git clone --depth 1, extract HEAD SHA)
     |
     v
 FORK_JOIN ─┬─ cicd_unit_test        (mvn test / gradle test / java -version)
            ├─ cicd_integration_test  (HTTP connectivity to github.com, google.com, httpbin.org)
            └─ cicd_security_scan     (regex scan for secrets, sensitive files, insecure HTTP)
     |
     JOIN (wait for all three)
     |
     v
cicd_deploy_staging  (create manifest.json + VERSION + health.json)
     |
     v
cicd_deploy_prod     (validate build artifacts exist, then deploy)
```

## Worker: Build (`cicd_build`)

Executes a real `git clone --depth 1 --branch {branch}` via `ProcessBuilder` into a temp directory. The process has a 120-second timeout with `destroyForcibly()` on expiration. After cloning, a second `ProcessBuilder` runs `git rev-parse --short HEAD` inside the clone to extract the actual commit SHA for image tagging.

```java
ProcessBuilder pb = new ProcessBuilder(
    "git", "clone", "--depth", "1", "--branch", branch, repoUrl, cloneTarget.toString());
pb.redirectErrorStream(true);
Process proc = pb.start();
boolean completed = proc.waitFor(120, java.util.concurrent.TimeUnit.SECONDS);
```

Build IDs are deterministic: `BLD-{hash(repoUrl + branch + shortSha) % 900000 + 100000}`. Image tags follow `{repoName}:{branch}-{shortSha}`. The repo name is extracted by stripping `.git` and everything before the last `/`.

## Worker: UnitTest (`cicd_unit_test`)

Auto-detects the build tool by checking for `pom.xml` (Maven) or `build.gradle` (Gradle) in the build directory. Falls back to `java -version` when no build file exists. Parses Maven's `Tests run: X, Failures: Y, Errors: Z, Skipped: W` output with a compiled regex to extract structured test counts. Output is truncated to the last 2000 characters to avoid flooding Conductor's task output storage.

## Worker: SecurityScan (`cicd_security_scan`)

Walks the build directory up to 10 levels deep, skipping `.git/`, `node_modules/`, and `target/`. Three detection layers:

**Hardcoded secrets (CRITICAL):** Regex `(?i)(password|secret|api[_-]?key|token|credential)\s*[=:]\s*["']?[A-Za-z0-9_+/=\-]{8,}` -- matches `password = "supersecret"` but not `password = ""`.

**Sensitive files (HIGH):** Exact filename match against `.env`, `.env.local`, `.env.production`, `credentials.json`, `id_rsa`, `id_ed25519`, `.pem`, `secret`, `keystore.jks`.

**Insecure HTTP (LOW):** `http://` URLs excluding localhost variants (`127.0.0.1`, `0.0.0.0`, `[::1]`).

The `blockDeploy` flag is set to `true` when `critical > 0`, providing a machine-readable gate for downstream deployment decisions.

## Worker: DeployProd (`cicd_deploy_prod`)

Before creating deployment artifacts, validates that the build directory exists on disk:

```java
if (buildDir != null && !buildDir.isBlank()) {
    Path buildPath = Path.of(buildDir);
    if (!Files.exists(buildPath)) {
        result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
        result.setReasonForIncompletion("Build artifacts not found at: " + buildDir);
        return result;
    }
}
```

Both staging and production deployments create a real directory under `java.io.tmpdir` containing `manifest.json` (with buildId, imageTag, environment, timestamp, and `promotedFrom: "staging"` for prod), a `VERSION` file, and a `health.json` endpoint stub.

## Error Handling

| Scenario | Worker | Status | Retryable? |
|---|---|---|---|
| Missing repoUrl or branch | Build | `FAILED_WITH_TERMINAL_ERROR` | No |
| Git clone timeout (>120s) | Build | `COMPLETED` with `cloneError` | Worker completes, but output signals failure |
| Build dir missing on deploy | DeployProd | `FAILED_WITH_TERMINAL_ERROR` | No |
| Critical secret found | SecurityScan | `COMPLETED` with `blockDeploy=true` | N/A -- gate, not failure |
| Test execution exception | UnitTest | `COMPLETED` with `failed=1, tool="error"` | Worker completes with error data |

## Test Coverage

5 test classes, 32 tests total:

**BuildTest (13 tests):** Missing/blank repoUrl, missing branch, deterministic buildId, null commitSha handling, repo name extraction from URL (`.git` suffix, no suffix, null).

**SecurityScanTest (10 tests):** Empty directory produces zero findings, `.env` file detection, hardcoded API key detection, critical finding sets `blockDeploy=true`, clean Java file does not block.

**DeployProdTest (9 tests):** Missing buildId/imageTag, nonexistent build directory rejection, real directory creation, manifest content validation (contains buildId, imageTag, "production").

**CiCdIntegrationTest (3 tests):** Full pipeline data flow (Build -> UnitTest -> SecurityScan -> DeployProd), security scan blocking deploy on critical finding, deploy failure on nonexistent artifacts.

---

## Production Notes

See [PRODUCTION.md](PRODUCTION.md) for deployment guidance, monitoring expectations, and security considerations.

---

> **How to run this example:** See [RUNNING.md](../../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
