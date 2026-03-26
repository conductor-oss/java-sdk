# Error Notification

An order processing system needs to alert the on-call team through multiple channels -- Slack and email -- the instant a workflow fails. The alerts must fire in parallel so neither channel blocks the other.

## Workflow

```
en_process_order ──(fails when shouldFail=true)──> failureWorkflow triggers:

    FORK ──┬── en_send_slack ──┐
           └── en_send_email ──┤
                               JOIN
```

The primary workflow `order_with_alerts` runs `en_process_order` and sets `failureWorkflow` to `"error_notification"`. The secondary workflow `error_notification` uses a `FORK_JOIN` to dispatch Slack and email notifications in parallel, then a `JOIN` on `en_send_slack_ref` and `en_send_email_ref`.

## Workers

**ProcessOrderWorker** (`en_process_order`) -- reads `shouldFail` from task input. When `shouldFail` is `true`, returns `FAILED` with `error` = `"Order processing failed"`. Otherwise returns `COMPLETED` with `result` = `"order-processed"`.

**SendSlackWorker** (`en_send_slack`) -- reads `channel` from input, defaulting to `"#alerts"`. Returns `sent` = `true` and the resolved `channel` name.

**SendEmailWorker** (`en_send_email`) -- reads `to` from input, defaulting to `"oncall@example.com"`. Returns `sent` = `true` and the recipient address.

**SendPagerDutyWorker** (`en_send_pagerduty`) -- a standalone alert worker that returns `sent` = `true`. Available for escalation scenarios but not wired into the default notification workflow.

## Workflow Output

The workflow produces `result` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `order_with_alerts` defines 1 task with input parameters `shouldFail` and a timeout of `120` seconds.

## Tests

4 tests verify successful order processing, failure triggering, and parallel notification dispatch through both Slack and email channels.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
