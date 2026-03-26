# Fraud Detection -- Production Deployment Guide

## Required Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `CONDUCTOR_BASE_URL` | Yes | `http://localhost:8080/api` | Conductor server API endpoint |
| `FRAUD_BLOCK_THRESHOLD` | No | `0.7` | Combined score above which transactions are blocked (0.0-1.0) |
| `FRAUD_REVIEW_THRESHOLD` | No | `0.4` | Combined score above which transactions require manual review (0.0-1.0) |
| `FRAUD_RULE_HIGH_RISK_WEIGHT` | No | `3.0` | Total rule weight at or above which the rule engine returns `high_risk` |
| `FRAUD_RULE_MEDIUM_RISK_WEIGHT` | No | `1.0` | Total rule weight at or above which the rule engine returns `medium_risk` |

## Security Considerations

- **No secrets in code**: All configuration is via environment variables. Never hardcode API keys or credentials.
- **Input validation**: All workers validate inputs and reject invalid data with `FAILED_WITH_TERMINAL_ERROR`. Negative amounts, missing IDs, and out-of-range scores are rejected before processing.
- **In-memory state**: The `AnalyzeTransactionWorker` and `VelocityCheckWorker` maintain in-memory customer history. In production, replace with a persistent store (Redis, database) to survive restarts and scale horizontally.
- **PCI compliance**: Transaction amounts and merchant IDs flow through Conductor task I/O. Ensure your Conductor deployment encrypts data at rest and in transit. Do not log full card numbers or CVVs.
- **Threshold tuning**: Block/review thresholds should be tuned per business risk appetite. Start conservative (lower thresholds = more blocks) and relax based on false-positive analysis.

## Deployment Notes

- **Horizontal scaling**: Each worker is stateless (aside from in-memory history caches). Deploy multiple instances behind a Conductor task poller. For production, externalize state to Redis.
- **Worker thread count**: Default is 1 thread per worker type. For high-throughput deployments, increase via `TaskRunnerConfigurer.Builder.withThreadCount()`.
- **Workflow registration**: The workflow definition (`workflow.json`) must be registered before starting workflows. The `FraudDetectionExample.main()` handles this, but in production use CI/CD to register definitions.
- **JVM settings**: Recommended `-Xmx512m -Xms256m` for worker processes. Increase if maintaining large in-memory customer histories.
- **Container deployment**: Use the provided `Dockerfile` and `docker-compose.yml`. Ensure `CONDUCTOR_BASE_URL` points to your Conductor cluster, not localhost.

## Monitoring Expectations

- **Task completion rate**: All 5 workers should have >99.9% completion rate. Monitor `FAILED_WITH_TERMINAL_ERROR` rate -- these indicate bad input data that will not resolve on retry.
- **Decision distribution**: Track the APPROVE/REVIEW/BLOCK ratio over time. A sudden shift indicates either a fraud wave or a threshold misconfiguration.
- **Latency**: Each worker should complete in <100ms. The full workflow (analyze + fork/join 3 checks + decide) should complete in <2 seconds.
- **Velocity check state size**: Monitor memory usage of VelocityCheckWorker. In production, implement TTL-based eviction or use Redis with expiry.
- **Rule firing rates**: Track which rules fire most frequently via the `rulesFired` output. Rules that never fire may indicate stale thresholds.
- **ML confidence**: Monitor the `confidence` field from MlScoreWorker. Low confidence (<0.7) indicates insufficient feature data.
