# Clinical Trials -- Production Deployment Guide

## Required Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `CONDUCTOR_BASE_URL` | Yes | `http://localhost:8080/api` | Conductor server API endpoint |

## Security Considerations

- **21 CFR Part 11 compliance**: Every worker outputs a `cfr11AuditTrail` map containing: timestamp, action, performedBy, participantId, electronicSignature, and reason. These records must be stored immutably for regulatory compliance. Do not allow deletion or modification of audit trails.
- **Electronic signatures**: Each worker generates an electronic signature for its action. In production, integrate with a validated e-signature system (DocuSign, Adobe Sign) for consent collection.
- **Patient data protection (HIPAA)**: Participant IDs, ages, conditions, and trial assignments are PHI. Ensure all data is encrypted at rest and in transit. Implement role-based access controls. Log all access to patient data.
- **Randomization integrity**: The randomization worker uses `SecureRandom` for unbiased group assignment. In production, use a validated randomization service that maintains allocation concealment (IRT/IXRS system).
- **Consent verification**: The consent worker generates SHA-256 signature hashes. In production, verify that consent was truly given before proceeding to randomization.

## Deployment Notes

- **Screening criteria**: Eligible conditions and exclusion criteria are defined as static sets. In production, externalize to a protocol-specific configuration file that can be updated per trial without code changes.
- **Age validation**: Participants must be 18-65. Ages outside 0-150 are rejected as terminal errors. The age range should be configurable per protocol.
- **Monitoring data**: The monitor worker generates simulated data. In production, integrate with EDC (Electronic Data Capture) systems like Medidata Rave or Oracle Clinical.
- **Statistical analysis**: The p-value calculation uses an effect-size approximation. In production, use validated statistical software (SAS, R validated packages) for regulatory submissions.
- **Workflow definition**: Register `workflow.json` before starting workflows. Each trial protocol should have its own workflow version.

## Monitoring Expectations

- **Screen-to-enroll ratio**: Track the percentage of screened patients who are eligible and consented. Low ratios indicate overly restrictive criteria or recruitment challenges.
- **Group balance**: Monitor treatment vs control allocation ratio. Significant deviation from 1:1 indicates a randomization issue.
- **Adverse event rate**: Track adverse events per participant. Rates exceeding protocol-defined thresholds should trigger Data Safety Monitoring Board (DSMB) review.
- **Compliance rates**: Monitor participant compliance (visit attendance, medication adherence). Rates below 70% may compromise trial validity.
- **Audit trail completeness**: Every workflow execution must have cfr11AuditTrail entries from all workers. Missing entries are 21 CFR Part 11 violations.
- **Terminal error rate**: `FAILED_WITH_TERMINAL_ERROR` indicates invalid patient data (missing age, invalid condition). These should be near zero in production.
- **Safety signals**: Monitor the `safetyAcceptable` flag from the analyze worker. Safety failures should trigger immediate DSMB notification.
