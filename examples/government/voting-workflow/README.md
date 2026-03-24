# Voting Workflow

Orchestrates voting workflow through a multi-stage Conductor workflow.

**Input:** `voterId`, `electionId`, `precinct` | **Timeout:** 60s

## Pipeline

```
vtw_register
    │
vtw_verify_identity
    │
vtw_cast_ballot
    │
vtw_count
    │
vtw_certify
```

## Workers

**CastBallotWorker** (`vtw_cast_ballot`)

Reads `electionId`. Outputs `ballotId`, `recorded`.

**CertifyWorker** (`vtw_certify`)

Reads `electionId`, `totalVotes`. Outputs `certified`, `certificationId`.

**CountWorker** (`vtw_count`)

Reads `ballotId`. Outputs `totalVotes`, `counted`.

**RegisterWorker** (`vtw_register`)

Reads `precinct`, `voterId`. Outputs `registered`.

**VerifyIdentityWorker** (`vtw_verify_identity`)

Reads `voterId`. Outputs `verified`.

## Tests

**3 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
