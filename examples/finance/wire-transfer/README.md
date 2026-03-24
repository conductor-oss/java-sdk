# Wire Transfer

Wire transfer workflow: validate, verify sender, compliance check, execute, and confirm.

**Input:** `transferId`, `senderAccount`, `recipientAccount`, `amount`, `currency` | **Timeout:** 60s

## Pipeline

```
wir_validate
    │
wir_verify_sender
    │
wir_compliance_check
    │
wir_execute
    │
wir_confirm
```

## Workers

**ComplianceCheckWorker** (`wir_compliance_check`)

Reads `amount`. Outputs `cleared`, `ctrRequired`, `sanctionsCleared`, `riskScore`.

**ConfirmWorker** (`wir_confirm`)

Reads `transactionRef`, `transferId`. Outputs `confirmed`, `confirmationNumber`, `notifiedParties`.

**ExecuteWorker** (`wir_execute`)

Reads `amount`, `currency`. Outputs `transactionRef`, `network`, `settledAt`, `fee`.

**ValidateWorker** (`wir_validate`)

Reads `amount`, `currency`, `transferId`. Outputs `valid`, `routingValid`, `recipientBankVerified`, `swiftCode`.

**VerifySenderWorker** (`wir_verify_sender`)

Reads `amount`, `senderAccount`. Outputs `verified`, `balance`, `sufficientFunds`, `authorizationLevel`.

## Tests

**3 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
