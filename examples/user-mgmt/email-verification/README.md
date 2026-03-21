# Email Verification in Java Using Conductor

## The Problem

You need to verify a user's email address after registration. Generating a unique verification code, sending it to the user's email, waiting for them to submit the code, checking that the submitted code matches the expected one, and activating their account only if verification succeeds. Each step depends on the previous one's output.

If the code is sent but the verification step compares against a stale or wrong expected code, legitimate users get locked out of their own accounts. If verification succeeds but account activation fails, the user sees "verified" but can't log in. Without orchestration, you'd build a monolithic verification handler that mixes code generation, email delivery, code comparison, and account state updates. Making it impossible to add code expiration, support SMS as an alternative channel, or track how long users take to verify for conversion analytics.

## The Solution

**You just write the code-generation, code-verification, and account-activation workers. Conductor handles the verification sequence and code-matching data flow.**

SendCodeWorker generates a unique verification code and emails it to the user's address, returning the expected code for later comparison. WaitInputWorker simulates the user receiving the email and submitting their verification code. In production, this would be a WAIT task that pauses until the user clicks the verification link or enters the code. VerifyCodeWorker compares the submitted code against the expected code and returns whether they match. ActivateAccountWorker flips the user's account status to active only if verification succeeded, completing the email ownership proof. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

SendCodeWorker generates and emails a verification code, WaitInputWorker simulates the user submitting it, VerifyCodeWorker compares codes, and ActivateAccountWorker flips the account to active on match.

| Worker | Task | What It Does |
|---|---|---|
| **ActivateAccountWorker** | `emv_activate` | Activates the user account after email verification. |
| **SendCodeWorker** | `emv_send_code` | Sends a verification code to the user's email. |
| **VerifyCodeWorker** | `emv_verify` | Verifies the submitted code against the expected code. |
| **WaitInputWorker** | `emv_wait_input` | Simulates waiting for user to submit their verification code. |

Replace with real identity provider and database calls and ### The Workflow

```
emv_send_code
 │
 ▼
emv_wait_input
 │
 ▼
emv_verify
 │
 ▼
emv_activate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
