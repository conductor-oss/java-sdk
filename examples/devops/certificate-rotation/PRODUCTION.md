# Certificate Rotation — Production Deployment Guide

## Required Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `CONDUCTOR_BASE_URL` | Yes | Conductor server API endpoint |

No API keys are required for this example. All workers use Java's built-in TLS and crypto libraries.

## Certificate Rotation Scheduling

### Recommended Schedule

| Cert Type | Rotation Period | Threshold Alert |
|-----------|----------------|-----------------|
| Internal (mTLS) | Every 90 days | Alert at 30 days remaining |
| Public (web) | Every 60 days (Let's Encrypt = 90 day validity) | Alert at 14 days remaining |
| Wildcard | Every 90 days | Alert at 30 days remaining |
| Code signing | Every 365 days | Alert at 60 days remaining |

### Cron-Based Scheduling

Use Conductor's scheduled workflows or external cron to trigger the rotation pipeline:

```bash
# Daily check at 2 AM UTC
0 2 * * * curl -X POST $CONDUCTOR_BASE_URL/workflow/cert_rotation_workflow -d '{"domain":"your-domain.com"}'
```

### Multi-Domain Rotation

For rotating certificates across multiple domains, trigger one workflow per domain or use a FORK_JOIN to check all domains in parallel.

## Error Handling

Workers classify errors as:
- **Terminal** (`FAILED_WITH_TERMINAL_ERROR`): DNS resolution failure, missing inputs — no retry
- **Retryable** (`FAILED`): Network timeout, connection refused — Conductor retries per task definition
- **Retryable** (`FAILED`): TLS handshake failure — may be transient

## Security Considerations

- The DiscoverWorker and VerifyWorker use a trust-all TLS manager to inspect certificates regardless of validity. This is intentional for cert inspection but must NOT be used for application traffic.
- In production, replace the self-signed GenerateWorker with a real CA integration:
  - **Let's Encrypt**: Use ACME protocol via certbot or acme4j
  - **AWS ACM**: Use AWS SDK to request/import certificates
  - **HashiCorp Vault**: Use Vault PKI secrets engine
- Store private keys in an HSM or secrets manager, never on disk
- The example keystore password (`changeit`) must be replaced with a strong, rotated password

## Deployment

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
mvn clean package -DskipTests
java -jar target/certificate-rotation-1.0.0.jar
```

## Monitoring

- Alert when `cr_discover` reports `expiringSoon=true`
- Alert when `cr_discover` reports `daysRemaining < 7` (critical)
- Monitor `cr_deploy` failures (keystore write failures)
- Monitor `cr_verify` failures after deployment (indicates deployment did not take effect)
- Track certificate serial numbers for audit compliance
- Log all rotation events for SOC2/SOX compliance

## Rollback

If a newly deployed certificate causes issues:
1. Keep the previous keystore file as a backup before deployment
2. Use Conductor's workflow retry/restart to re-run with the previous certificate
3. Implement a separate rollback workflow that restores from backup keystore
