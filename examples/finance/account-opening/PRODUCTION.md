# Account Opening -- Production Deployment Guide

## Required Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `CONDUCTOR_BASE_URL` | Yes | `http://localhost:8080/api` | Conductor server API endpoint |

## Security Considerations

- **SSN handling**: SSN format is validated (XXX-XX-XXXX). SSNs with all-zero groups are rejected. In production, SSNs must be encrypted at rest and masked in logs (show only last 4 digits).
- **Date of birth validation**: Applicants must be at least 18 years old. Future dates and dates over 120 years old are rejected.
- **Identity verification**: The VerifyIdentityWorker checks that driver's license, SSN, and proof of address documents are present. In production, integrate with a real KYC provider (Jumio, Onfido, etc.).
- **ChexSystems**: The credit check uses a hash-based score. In production, integrate with the real ChexSystems API. Scores >= 700 result in rejection.
- **Compliance audit trail**: Every worker outputs an `auditTrail` map containing timestamp, action, and decision details. Store these permanently for regulatory compliance.
- **Account numbers**: Generated via `SecureRandom` to prevent predictability. In production, use your core banking system's account number assignment.
- **PII protection**: Applicant names, SSNs, and dates of birth flow through Conductor task I/O. Ensure encryption at rest and in transit. Implement data retention policies.

## Deployment Notes

- **KYC integration**: Replace the deterministic identity verification with real KYC provider calls. Handle provider outages as retryable errors (network timeout = FAILED, auth failure = FAILED_WITH_TERMINAL_ERROR).
- **Credit bureau integration**: Replace hash-based ChexSystems scores with real API calls. Cache results to avoid duplicate bureau hits.
- **Core banking**: Replace in-memory account creation with real core banking API calls for account provisioning.
- **Workflow definition**: Register `workflow.json` before starting workflows.

## Monitoring Expectations

- **Approval rate**: Track the percentage of applications that result in account opening. Low rates may indicate overly strict criteria or data quality issues.
- **Rejection reasons**: Monitor the distribution of rejection reasons (identity failure, high ChexSystems score, insufficient deposit). Spikes indicate fraud patterns or partner issues.
- **Audit trail completeness**: Verify that every workflow execution has audit trail entries from all workers. Missing audit trails are compliance violations.
- **Terminal error rate**: `FAILED_WITH_TERMINAL_ERROR` indicates bad input data. These should be near zero -- they indicate upstream form validation failures.
- **KYC provider latency**: In production, monitor identity verification response times. Timeouts should trigger retries, not permanent failures.
