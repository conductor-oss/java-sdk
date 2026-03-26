# Idempotent Operations with Duplicate Detection via SWITCH

Retries in a distributed system can cause the same operation to execute twice. This workflow
generates an idempotency key, checks if it has been seen before, then routes via SWITCH:
new operations execute and record completion, while duplicates return the cached result.

## Workflow

```
operationId, action, data
           |
           v
+-------------------+     +----------------------+
| io_generate_key   | --> | io_check_duplicate   |
+-------------------+     +----------------------+
  key generated               isDuplicate: false
           |
           v
      SWITCH on isDuplicate
      +--false (new)----------+--true (duplicate)--+
      | io_execute            | (return cached)    |
      | result: "success"     |                    |
      |       |               |                    |
      |       v               |                    |
      | io_record_completion  |                    |
      | recorded: true        |                    |
      +-----------------------+--------------------+
```

## Workers

**GenerateKeyWorker** -- Generates an idempotency `key` from the operation inputs.

**CheckDuplicateWorker** -- Checks if the key has been seen. Returns `isDuplicate: false`
for new operations.

**ExecuteWorker** -- Executes the `action` with the idempotency `key`. Returns
`result: "success"`.

**RecordCompletionWorker** -- Stores the completion record for the key. Returns
`recorded: true`.

## Tests

8 unit tests cover key generation, duplicate detection, execution, and completion recording.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
