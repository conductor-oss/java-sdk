# GDPR Compliance -- Production Deployment Guide

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `CONDUCTOR_BASE_URL` | Yes | `http://localhost:8080/api` | Conductor server API endpoint |
| `CONDUCTOR_AUTH_KEY` | If auth enabled | none | API key for Conductor authentication |
| `CONDUCTOR_AUTH_SECRET` | If auth enabled | none | API secret for Conductor authentication |

## Security Considerations

- **PII handling**: All PII detection patterns run in-memory. No PII data is persisted to disk or logs. The `maskValue()` and `maskPII()` methods ensure only redacted forms appear in output.
- **Audit logging**: Every worker produces an audit log entry with `timestamp`, `action`, `actor`, `result`, and `detail`. Forward these to your SIEM/audit system.
- **Transport**: Use HTTPS for `CONDUCTOR_BASE_URL` in production. Never use plain HTTP.
- **Input validation**: All workers fail with `FAILED_WITH_TERMINAL_ERROR` on missing or invalid inputs. No fallback/synthetic data is generated.
- **Data subject IDs**: Treat `subjectId` and `requestorId` as sensitive. Do not log them in external systems without masking.

## Deployment

1. Build the artifact:
   ```bash
   mvn clean package -DskipTests
   ```

2. Run workers:
   ```bash
   export CONDUCTOR_BASE_URL=https://your-conductor:8080/api
   java -jar target/gdpr-compliance-1.0.0.jar --workers
   ```

3. Docker:
   ```bash
   docker build -t gdpr-compliance .
   docker run -e CONDUCTOR_BASE_URL=https://your-conductor:8080/api gdpr-compliance
   ```

## Monitoring

- **Task failure rate**: Alert on `FAILED_WITH_TERMINAL_ERROR` status for any GDPR task. These indicate invalid inputs or configuration errors that require human intervention.
- **PII detection metrics**: Track `piiCount` output from `gdpr_process_request` to monitor data exposure trends.
- **Audit log completeness**: Every workflow execution should produce exactly 4 audit log entries (verify, locate, process, confirm). Alert on missing entries.
- **SLA**: GDPR requires responding to data subject requests within 30 days. Track workflow start-to-completion time.
- **Worker health**: Monitor Conductor task queue depth for `gdpr_*` tasks. Rising queues indicate workers are down or overloaded.

## Error Classification

| Error | Type | Action |
|---|---|---|
| Missing `requestorId` | Terminal | Fix caller input |
| Missing `dataSources` | Terminal | Configure data source registry |
| Invalid `requestType` | Terminal | Fix caller input (must be: access, erasure, anonymize, portability) |
| Missing `email` | Terminal | Fix caller input |
| Negative `piiCount` | Terminal | Bug in upstream worker |
| Conductor connection error | Retryable | Check network/server health |
