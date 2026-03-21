# Implementing Authentication Workflow in Java with Conductor : Credential Validation, MFA Verification, Risk Assessment, and Token Issuance

## The Problem

You need to authenticate users through multiple verification layers before granting access. First, the submitted credentials (password hash, biometric template, or API key) must be validated against the identity store. Then, if MFA is enabled, a second factor (TOTP, SMS code, hardware key) must be verified. Before issuing a token, a risk assessment must evaluate whether the login attempt looks suspicious. new device, unusual geolocation, impossible travel, or velocity anomalies. Only after all three checks pass should a JWT be minted with the appropriate claims, scopes, and expiration. If any step fails, the login must be denied and the failure recorded for security monitoring.

Without orchestration, authentication logic is a deeply nested chain of if/else blocks. validate credentials, then check MFA, then assess risk, then issue a token. Each step calls a different backend (identity provider, MFA service, risk engine, token service), and a failure in one requires careful cleanup of the others. When the MFA provider times out, users get stuck in a half-authenticated state. When the risk engine is slow, login latency spikes. Nobody can tell from logs which step actually failed for a given login attempt, making it impossible to debug "why can't I log in?" support tickets.

## The Solution

**You just write the credential validation, MFA check, and token signing logic. Conductor handles strict ordering so no token is minted without MFA and risk checks, retries when the MFA provider is temporarily unavailable, and a full audit trail of every login attempt.**

Each authentication concern is a simple, independent worker. one validates credentials against the identity store, one verifies the MFA challenge, one scores login risk from device and location signals, one mints the JWT with appropriate claims. Conductor takes care of executing them in strict order so no token is issued without MFA verification and risk assessment, retrying if the MFA provider is temporarily unavailable, and maintaining a complete audit trail of every login attempt with inputs, outputs, and timing for each verification step. ### What You Write: Workers

The authentication pipeline sequences ValidateCredentialsWorker to check passwords or biometrics, CheckMfaWorker to verify the second factor, RiskAssessmentWorker to evaluate device and location signals, and IssueTokenWorker to mint a signed JWT only after all checks pass.

| Worker | Task | What It Does |
|---|---|---|
| **ValidateCredentialsWorker** | `auth_validate_credentials` | Validates the submitted credentials (password hash, biometric template, or API key) against the identity store and returns a success/failure result with the user ID |
| **CheckMfaWorker** | `auth_check_mfa` | Verifies the second authentication factor. TOTP code, SMS one-time password, or hardware security key challenge, and confirms MFA status |
| **RiskAssessmentWorker** | `auth_risk_assessment` | Scores the login attempt for risk signals. device fingerprint, geolocation, impossible travel, login velocity, and returns a risk level (low/medium/high) |
| **IssueTokenWorker** | `auth_issue_token` | Mints a signed JWT with user claims, scopes, and expiration after all verification steps have passed |

the workflow logic stays the same.

### The Workflow

```
auth_validate_credentials
 │
 ▼
auth_check_mfa
 │
 ▼
auth_risk_assessment
 │
 ▼
auth_issue_token

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
