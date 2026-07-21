# Schedules and events

**Audience:** teams starting workflows from time-based or event-driven triggers.  
**Works with:** schedules work in OSS and Orkes; event infrastructure depends on server configuration.

Use the typed `SchedulerClient` for schedule metadata and the workflow `EVENT` task or event handlers for asynchronous events. The Java API map links each client and its canonical Javadocs.

## Schedule safely

Use a stable correlation identifier derived from the time window so a retry or double delivery cannot create duplicate business work. Keep the scheduled workflow idempotent and make its input small: pass object references, not large files.

**Fragment — create the client from `OrkesClients`; the [API map](api-map.md) links the complete signature reference.**

```java
StartWorkflowRequest start = new StartWorkflowRequest()
        .withName("billing")
        .withVersion(2)
        .withCorrelationId("billing-2026-07-19")
        .withInput(Map.of("billingDate", "2026-07-19"));

SaveScheduleRequest schedule = new SaveScheduleRequest()
        .name("daily-billing")
        .cronExpression("0 0 2 * * ?")
        .zoneId("UTC")
        .startWorkflowRequest(start);

schedulerClient.saveSchedule(schedule);
```

Expected result: the schedule is visible through the server API/UI and produces a workflow execution at the configured time.

## Event-driven work

Use an `EVENT` task to publish from a workflow, or configure an event handler to start a workflow. Put event payload validation at the workflow boundary and include an idempotency key from the producer.

Next: [workflow lifecycle](workflow-lifecycle.md), [reliability](reliability.md), and [debugging](debugging.md).
