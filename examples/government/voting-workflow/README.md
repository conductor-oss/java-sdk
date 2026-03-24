# Voting Workflow in Java with Conductor

Processes a voter's participation in an election: confirming registration, verifying identity, casting the ballot, counting the vote, and certifying results.

## The Problem

You need to process a voter's participation in an election. from registration verification to vote certification. The voter's registration is confirmed, their identity is verified against the voter roll, they cast their ballot, the vote is counted, and the results are certified. Every step must maintain ballot secrecy while ensuring auditability. Counting a vote from an unverified voter undermines election integrity; failing to count a legitimate vote disenfranchises a citizen.

Without orchestration, you'd manage the voting process through a combination of poll books, voting machines, and tally systems. manually verifying registrations against printed rolls, handling provisional ballots when verification fails, reconciling machine counts with paper trails, and producing certified results under strict legal deadlines.

## The Solution

**You just write the registration confirmation, identity verification, ballot casting, vote counting, and result certification logic. Conductor handles verification retries, ballot sequencing, and election audit trails.**

Each voting concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (register, verify identity, cast ballot, count, certify), maintaining a complete audit trail while preserving ballot secrecy, tracking every step with timestamps, and resuming from the last step if the process crashes.

### What You Write: Workers

Voter verification, ballot issuance, vote recording, and tally workers handle election processes with independent, auditable steps.

| Worker | Task | What It Does |
|---|---|---|
| **CastBallotWorker** | `vtw_cast_ballot` | Records the ballot casting event for the election and assigns a ballot ID |
| **CertifyWorker** | `vtw_certify` | Certifies the election results with the total vote count and issues a certification ID |
| **CountWorker** | `vtw_count` | Tabulates the ballot and adds it to the running vote total for the election |
| **RegisterWorker** | `vtw_register` | Confirms the voter's registration at their assigned precinct |
| **VerifyIdentityWorker** | `vtw_verify_identity` | Verifies the voter's identity against the voter roll |

### The Workflow

```
vtw_register
 │
 ▼
vtw_verify_identity
 │
 ▼
vtw_cast_ballot
 │
 ▼
vtw_count
 │
 ▼
vtw_certify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
