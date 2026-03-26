# Circuit Breaker -- Production Deployment Guide

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `CONDUCTOR_BASE_URL` | Yes | `http://localhost:8080/api` | Conductor server API endpoint |
| `CONDUCTOR_AUTH_KEY` | If auth enabled | none | API key for Conductor authentication |
| `CONDUCTOR_AUTH_SECRET` | If auth enabled | none | API secret for Conductor authentication |

## Security Considerations

- **State persistence**: Circuit breaker state is persisted to files under `java.io.tmpdir/circuit-breaker-state/`. In production, ensure this directory has appropriate permissions. For multi-instance deployments, use a shared store (Redis, database).
- **Failure threshold**: Default threshold is 3. Set this based on your SLOs. Too low causes flapping; too high delays detection.
- **Service name validation**: Service names are required. Workers fail with `FAILED_WITH_TERMINAL_ERROR` on missing/blank service names.

## Deployment

1. Build:
   ```bash
   mvn clean package -DskipTests
   ```

2. Run workers:
   ```bash
   export CONDUCTOR_BASE_URL=https://your-conductor:8080/api
   java -jar target/circuit-breaker-1.0.0.jar --workers
   ```

## Monitoring

- **Circuit state transitions**: Track state changes (CLOSED -> OPEN, OPEN -> HALF_OPEN, HALF_OPEN -> CLOSED/OPEN). Alert on OPEN transitions.
- **Failure counts**: Monitor `failureCount` output from `cb_call_service`. Rising counts indicate service degradation.
- **Fallback usage**: Track how often `cb_fallback` is invoked. High fallback rates mean the upstream service is unhealthy.
- **Half-open success rate**: Monitor success/failure of HALF_OPEN probe calls. Low success rates mean the service is not recovering.
- **Worker health**: Monitor Conductor task queue depth for `cb_*` tasks.

## State Machine

```
CLOSED --[failures >= threshold]--> OPEN
OPEN   --[cooldown expired]-------> HALF_OPEN
HALF_OPEN --[success]-------------> CLOSED
HALF_OPEN --[failure]-------------> OPEN
```

## Error Classification

| Error | Type | Action |
|---|---|---|
| Missing `serviceName` | Terminal | Fix caller input |
| Threshold <= 0 | Terminal | Fix caller configuration |
| Negative failureCount | Terminal | Bug in upstream logic |
| Service call failure | Terminal | Increment failure count, check circuit |
| Conductor connection error | Retryable | Check network/server health |
