# CI/CD Pipeline -- Production Deployment Guide

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `CONDUCTOR_BASE_URL` | Yes | `http://localhost:8080/api` | Conductor server API endpoint |
| `CONDUCTOR_AUTH_KEY` | If auth enabled | none | API key for Conductor authentication |
| `CONDUCTOR_AUTH_SECRET` | If auth enabled | none | API secret for Conductor authentication |
| `JAVA_HOME` | Yes | system default | Path to JDK installation |

## Security Considerations

- **Git credentials**: The Build worker runs `git clone`. Ensure git credentials are configured via SSH keys or credential helpers, not embedded in URLs.
- **Security scan**: The SecurityScan worker performs real file scanning for hardcoded secrets, sensitive files, and insecure URLs. Critical findings set `blockDeploy=true`.
- **Build isolation**: Build directories are created under `java.io.tmpdir`. In production, use isolated containers or sandboxed environments.
- **Deploy validation**: DeployProd validates that `buildDir` exists when provided. This prevents deploying non-existent artifacts.

## Deployment

1. Build:
   ```bash
   mvn clean package -DskipTests
   ```

2. Run workers:
   ```bash
   export CONDUCTOR_BASE_URL=https://your-conductor:8080/api
   export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
   java -jar target/ci-cd-pipeline-1.0.0.jar --workers
   ```

## Monitoring

- **Build failure rate**: Track `cloneExitCode` and `cloneError` from the Build worker. Non-zero exit codes indicate git issues.
- **Test pass rate**: Monitor `passed` and `failed` counts from UnitTest. Alert on `failed > 0`.
- **Security gate**: Alert when `blockDeploy=true` from SecurityScan. This means critical vulnerabilities were found.
- **Deploy success**: Track `deployed` boolean from DeployProd/DeployStaging. Alert on `deployed=false`.
- **Pipeline duration**: Sum `durationMs` from all workers to track total pipeline time. Alert on SLA breaches.
- **Worker health**: Monitor Conductor task queue depth for `cicd_*` tasks.

## Pipeline Stages

```
Build -> UnitTest -> SecurityScan -> DeployStaging -> IntegrationTest -> DeployProd
```

Each stage receives output from the previous stage:
- Build outputs `buildId`, `imageTag`, `buildDir`
- UnitTest uses `buildId` and `buildDir`
- SecurityScan uses `buildId` and `buildDir`
- DeployProd uses `buildId`, `imageTag`, and validates `buildDir` if provided

## Error Classification

| Error | Type | Action |
|---|---|---|
| Missing `repoUrl` | Terminal | Fix caller input |
| Missing `branch` | Terminal | Fix caller input |
| Missing `buildId` | Terminal | Fix upstream Build worker |
| Missing `imageTag` | Terminal | Fix upstream Build worker |
| Build artifacts not found | Terminal | Ensure Build step completed |
| Git clone failure | Retryable | Check network/credentials |
| Critical security finding | Gate | Review and fix before deploying |
| Conductor connection error | Retryable | Check network/server health |
