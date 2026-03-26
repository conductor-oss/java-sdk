# Secure Password Reset with PBKDF2 Hashing and Conductor's Secrets API

A user clicks "forgot password." The system must generate a cryptographically secure token, validate it has not expired, enforce password complexity rules, hash the new password with a proper key derivation function, and send the right notification -- without leaking the plaintext password into workflow history.

This example implements a four-stage pipeline with real `SecureRandom` token generation, `Instant`-based expiration checking, PBKDF2WithHmacSHA256 hashing (65,536 iterations), and Conductor's `workflow.secrets` API to keep the new password out of task logs.

## Pipeline

```
pwd_request  (email validation + SecureRandom token + 1-hour expiry)
     |
     v
pwd_verify   (token length check + Instant.parse expiration check)
     |
     v
pwd_reset    (strength scoring + PBKDF2WithHmacSHA256 hashing)
     |        newPassword sourced from ${workflow.secrets.reset_password}
     v
pwd_notify   (conditional email: success or failure message)
```

## The Secrets API Integration

The workflow wires the new password from Conductor's secrets store: `"newPassword": "${workflow.secrets.reset_password}"`. The plaintext password never appears in workflow input parameters, execution history, or task logs. It is resolved at runtime by Conductor's secrets engine and passed directly to the worker.

## Worker: RequestWorker (`pwd_request`)

Validates the email format against `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$` and generates a 256-bit token:

```java
byte[] tokenBytes = new byte[32];
SECURE_RANDOM.nextBytes(tokenBytes);
String resetToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
```

The token is URL-safe Base64 (no `+`, `/`, or `=`), making it safe to embed in a reset link without encoding. Expiration is set to 1 hour from request time. The userId is derived from `Integer.toHexString(email.hashCode())`, prefixed with `USR-`.

## Worker: VerifyTokenWorker (`pwd_verify`)

Two validation checks:

1. **Token length:** Must be at least 8 characters (the SecureRandom tokens from RequestWorker are 43 characters, but this catches truncated or manually entered tokens)
2. **Expiration:** Parses the `expiresAt` string via `Instant.parse()` and compares against `Instant.now()`

```java
Instant expiresAt = Instant.parse(expiresAtStr);
expired = Instant.now().isAfter(expiresAt);
```

Invalid `expiresAt` format is a terminal error. Expired tokens fail with "Reset token has expired." Short tokens fail with "Reset token is invalid (too short)." Both `verified` and `expired` booleans are always present in output for downstream decision-making.

## Worker: ResetWorker (`pwd_reset`) -- Password Strength and PBKDF2

### Gate Check

Before processing the password, the worker checks that `verified == true` from the previous step. If the token was not verified, the reset is rejected immediately -- even if a valid password is provided.

### Strength Scoring

Five criteria, each worth 1 point:

| Criterion | Check |
|---|---|
| Minimum length | `newPassword.length() >= 8` |
| Uppercase letter | `Character.isUpperCase` |
| Lowercase letter | `Character.isLowerCase` |
| Digit | `Character.isDigit` |
| Special character | Match against `!@#$%^&*()_+-=[]{}|;':\",./<>?` |

A password must score at least 3/5 AND meet the minimum length. "MyStr0ng!Pass" scores 5/5. "alllowercase" scores 2/5 (length + lowercase) and is rejected. "abc" scores 1/5 and is rejected.

### PBKDF2 Hashing

```java
byte[] saltBytes = new byte[16];
SECURE_RANDOM.nextBytes(saltBytes);
PBEKeySpec spec = new PBEKeySpec(newPassword.toCharArray(), saltBytes, 65536, 256);
SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
byte[] hash = factory.generateSecret(spec).getEncoded();
passwordHash = Base64.getEncoder().encodeToString(hash);
```

65,536 iterations of PBKDF2WithHmacSHA256 with a 128-bit random salt producing a 256-bit derived key. Both the hash and salt are Base64-encoded in the output for storage. The plaintext password is never stored or logged.

### Audit Trail

Every outcome -- success, weak password, unverified token -- includes `resetAt` (timestamp), `userId`, and `strength` (score) in the output. The worker never returns without these fields.

## Worker: NotifyWorker (`pwd_notify`)

Generates a conditional notification based on `resetSuccess`:

- **true:** Subject "Your password has been reset", body includes contact-support warning
- **false/missing:** Subject "Password reset failed", body suggests trying a stronger password

The `resetSuccess` flag defaults to `false` when missing, ensuring users are never silently left without notification.

## Error Handling

Every worker uses `FAILED_WITH_TERMINAL_ERROR` for non-retryable conditions: invalid email, expired token, unparseable `expiresAt`, short token, weak password, and unverified token. The only retryable failure is PBKDF2 algorithm unavailability (a JVM issue). Each failure halts the pipeline at the relevant step -- Conductor does not advance to the next worker.

## Test Coverage

5 test classes, 23 tests across four categories:

**Integration (4 tests):** Full pipeline success, expired token blocking pipeline, weak password rejected while notify still works, audit trail preserved across all steps.

**Token lifecycle (11 tests):** URL-safe Base64 token generation, email validation, `Instant.parse` expiration, short/missing/blank token rejection, userId passthrough.

**Password hashing (8 tests):** PBKDF2 hash + salt output, strength scoring for strong/weak/short/all-lowercase passwords, unverified token gate, audit fields on every outcome.

---

## Production Notes

See [PRODUCTION.md](PRODUCTION.md) for deployment guidance, monitoring expectations, and security considerations.

---

> **How to run this example:** See [RUNNING.md](../../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
