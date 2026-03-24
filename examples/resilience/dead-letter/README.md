# Dead Letter Queue

A data processing task receives messages that occasionally contain poison payloads. Rather than losing those failures silently, this workflow captures them into a dead-letter log file on disk and tracks them in memory for potential replay.

## Workflow

```
dl_process ──(fails when mode="fail")──> dl_handle_failure
```

Workflow `dead_letter_demo` accepts `mode` and `data` as input parameters. When `mode` is `"fail"`, the process task returns `FAILED`, and the failure handler records the details.

## Workers

**ProcessWorker** (`dl_process`) -- reads `mode` and `data` from task input. When `mode` equals `"fail"`, returns `FAILED` with output `error` = `"Processing failed for data: " + data`. Any other mode value produces `COMPLETED` with `result` = `"Processed: " + data`.

**HandleFailureWorker** (`dl_handle_failure`) -- accepts `failedWorkflowId`, `failedTaskName`, and `error` as inputs. Writes a timestamped entry to a temp file created via `Files.createTempFile("dead-letter-", ".log")` using `StandardOpenOption.APPEND`. Also stores the failure in a static `ConcurrentHashMap<String, String>` called `HANDLED_FAILURES` keyed by `workflowId + ":" + taskName`. Returns `handled` = `true`, the `summary` string, `writtenToFile` flag, and `totalTrackedFailures` count.

## Workflow Output

The workflow produces `result`, `mode` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 2 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `dead_letter_demo` defines 1 task with input parameters `mode`, `data` and a timeout of `120` seconds.

## Workflow Definition Details

Workflow description: "Dead letter queue demo -- dl_process task that can fail based on mode input. Failed tasks are captured for dead letter handling.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Implementation Notes

All workers implement the `com.netflix.conductor.client.worker.Worker` interface. Input parameters are read from `task.getInputData()` and output is written to `result.getOutputData()`. Workers return `TaskResult.Status.COMPLETED` on success and `TaskResult.Status.FAILED` on failure. The workflow JSON definition in `src/main/resources/` declares the task graph, input wiring via `${ref.output}` expressions, and output parameters.

## Tests

7 tests verify successful processing, intentional failure, dead-letter capture to file, in-memory tracking, and the complete round-trip from failure to logged entry.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
