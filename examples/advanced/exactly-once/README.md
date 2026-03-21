# Exactly-Once Processing in Java Using Conductor: Lock, Dedup, Process, Commit, Unlock

The payment service processes the $49.99 debit, then crashes before acknowledging the message. The broker retries. Another instance picks it up and processes it again, customer double-charged. You add an idempotency check, but two instances grab the same message simultaneously and both pass the check before either records it. You add a distributed lock, but the lock holder crashes mid-processing and the TTL expires before cleanup. Every fix opens a new failure mode. Exactly-once delivery is impossible, but exactly-once processing is achievable with the right protocol. This example implements the full lock-check-process-commit-unlock sequence with Conductor, ensuring the business effect happens once even when messages arrive twice. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Preventing Duplicate Processing in Distributed Systems

A payment message arrives twice. Once from the original send, once from a retry after a timeout. Without exactly-once semantics, the customer gets charged twice. A stock trade gets executed, the acknowledgment is lost, and the retry creates a duplicate position. In any system where the same message can arrive more than once, you need a way to guarantee that the business effect happens exactly once.

Exactly-once processing requires a careful protocol: acquire a distributed lock on the resource key (so no other consumer can process the same message concurrently), check the idempotency store to see if this message ID was already processed, execute the business logic only if it's new, atomically commit the result and record the message ID, then release the lock. If any step fails between lock and commit, the lock's TTL ensures eventual release, and the next retry will find the message uncommitted and reprocess it safely.

## The Solution

**You write the locking and deduplication logic. Conductor handles the protocol sequencing, retries, and state recovery.**

`ExoLockWorker` acquires a distributed lock on the resource key with a 30-second TTL and returns a lock token. `ExoCheckStateWorker` looks up the message ID in the idempotency store to determine if it's already been processed. `ExoProcessWorker` executes the business logic only if the state check shows the message is new. `ExoCommitWorker` atomically writes the result and marks the message ID as processed, using the lock token to verify it still holds the lock. `ExoUnlockWorker` releases the lock. Conductor ensures this five-step protocol executes in strict order, and if any step fails, the workflow can be retried from the point of failure without risking double-processing.

### What You Write: Workers

Five workers enforce the exactly-once protocol: distributed locking, state checking, business logic execution, atomic commit, and lock release, each owning one step of the deduplication boundary.

| Worker | Task | What It Does |
|---|---|---|
| **ExoCheckStateWorker** | `exo_check_state` | Checks whether the message was already processed by looking up its current state and sequence number |
| **ExoCommitWorker** | `exo_commit` | Atomically commits the processing result and records the state transition (pending to completed) |
| **ExoLockWorker** | `exo_lock` | Acquires a distributed lock with a TTL to prevent concurrent processing of the same message |
| **ExoProcessWorker** | `exo_process` | Executes the idempotent business logic (e.g., applying a debit and computing new balance) |
| **ExoUnlockWorker** | `exo_unlock` | Releases the distributed lock after processing and commit are complete |

### The Workflow

```
exo_lock
 │
 ▼
exo_check_state
 │
 ▼
exo_process
 │
 ▼
exo_commit
 │
 ▼
exo_unlock

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
