# Password Reset — Production Deployment Guide

## Required Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `CONDUCTOR_BASE_URL` | Yes | Conductor server API endpoint |

No external API keys required. All cryptographic operations use Java's built-in libraries.

## Password Policy

### Minimum Requirements (enforced by ResetWorker)

- Minimum 8 characters
- At least 3 of the following 4 categories:
  - Uppercase letter (A-Z)
  - Lowercase letter (a-z)
  - Digit (0-9)
  - Special character (!@#$%^&* etc.)

### Strength Scoring

| Score | Rating | Allowed? |
|-------|--------|----------|
| 0-2 | Weak | No |
| 3 | Moderate | Yes (minimum) |
| 4 | Strong | Yes |
| 5 | Very Strong | Yes |

### Recommended Production Enhancements

- Add password history check (prevent reuse of last N passwords)
- Add breach database check (HaveIBeenPwned API)
- Add dictionary word check
- Implement account lockout after N failed reset attempts

## Secret Rotation Schedule

| Secret | Rotation Period | Notes |
|--------|----------------|-------|
| Reset tokens | Single-use, 1-hour expiry | Tokens are cryptographically random (256-bit) |
| PBKDF2 salt | Per-password (unique) | 128-bit random salt per hash |
| Conductor API credentials | Every 90 days | If using Orkes Cloud with API keys |

## Security Considerations

- **Token generation**: Uses `SecureRandom` with 256 bits of entropy, Base64url-encoded
- **Password hashing**: PBKDF2 with HMAC-SHA256, 65536 iterations, 256-bit output
- **Token expiry**: 1 hour from generation; expired tokens are rejected with terminal error
- **Audit trail**: Every outcome (success or failure) includes `resetAt`, `userId`, and `strength` fields
- In production:
  - Store password hashes and salts in a secure database
  - Use Conductor Secrets API for sensitive workflow variables
  - Implement rate limiting on the request endpoint
  - Log all reset attempts for security audit

## Error Handling

- **Terminal** (`FAILED_WITH_TERMINAL_ERROR`):
  - Invalid email format
  - Expired reset token
  - Invalid/short reset token
  - Weak password
  - Unverified token
- **Retryable** (`FAILED`):
  - PBKDF2 hashing errors (rare JCE issues)

## Deployment

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
mvn clean package -DskipTests
java -jar target/password-reset-1.0.0.jar
```

## Monitoring

- Alert on `pwd_request` failures (email validation issues)
- Alert on `pwd_verify` failures with `expired=true` (users clicking expired links)
- Track password strength distribution from `pwd_reset` output
- Monitor `pwd_reset` failure rate (weak passwords) to tune UI password requirements
- Ensure every `pwd_reset` output contains audit fields: `resetAt`, `userId`, `strength`
