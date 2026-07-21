# Workflow lifecycle and versioning

**Audience:** teams publishing workflow definitions.  
**Works with:** OSS and Orkes.

Register definitions deliberately and treat their inputs and outputs as an API. Use [workflows](workflows.md) for the fluent builder surface.

## Safe evolution

1. Give every workflow a description, owner, timeout policy, and stable task reference names.
2. Keep existing input and output keys compatible. Additive changes are safest.
3. Publish a new workflow version for breaking graph or output changes; pin callers and sub-workflows to the intended version.
4. Leave the old version available while executions and callers still use it, then retire it after their retention window.

Do not mutate a running workflow's semantics in place: an execution can be between durable tasks while the definition changes.

## Register, run, inspect

**Fragment — use the fully runnable [Hello World](core-quickstart.md) for client construction and worker lifecycle.**

```java
workflow.registerWorkflow(true, true);
String workflowId = workflow.execute(Map.of("customerId", "c-123"));
```

Expected result: the definition is visible in the Conductor UI and the execution has a durable workflow ID. Query the status through `WorkflowClient` or the UI before changing a definition.

## Failure modes

- Missing task definition or worker: execution stops at a `SCHEDULED` task.
- Output rename: downstream tasks or callers see missing values.
- Unbounded work: a workflow timeout is absent or too permissive.

Next: [reliability](reliability.md), [schedules and events](schedules-events.md), and [upgrading](upgrading.md).
