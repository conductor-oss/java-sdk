# Scheduling

Run agents on a cron schedule. Schedules are stored in Conductor and survive server restarts — no cron daemon or external scheduler needed. Use the SDK's typed `SchedulerClient` directly.

## Deploy an agent with a schedule

```java
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;

import io.orkes.conductor.client.SchedulerClient;
import io.orkes.conductor.client.model.SaveScheduleRequest;

Agent reportAgent = Agent.builder()
    .name("daily_report")
    .model("anthropic/claude-sonnet-4-6")
    .instructions("Generate a daily sales summary.")
    .build();

try (AgentRuntime runtime = new AgentRuntime()) {
    runtime.deploy(reportAgent);

    StartWorkflowRequest workflow = new StartWorkflowRequest();
    workflow.setName(reportAgent.getName());
    SchedulerClient schedules = runtime.getSchedulerClient();
    schedules.saveSchedule(new SaveScheduleRequest()
        .name("daily_report-daily")
        .cronExpression("0 9 * * *") // 9 AM every day
        .zoneId("America/New_York")
        .startWorkflowRequest(workflow));
}
```

## Schedule with custom input

Pass a fixed prompt or parameters to the scheduled run:

```java
StartWorkflowRequest workflow = new StartWorkflowRequest();
workflow.setName("daily_report");
workflow.setInput(Map.of("report_type", "weekly", "include_charts", true));

SaveScheduleRequest weeklyDigest = new SaveScheduleRequest()
    .name("daily_report-weekly")
    .cronExpression("0 8 * * MON")
    .zoneId("UTC")
    .description("Monday morning executive digest")
    .startWorkflowRequest(workflow);
```

## Manage schedules

```java
SchedulerClient schedules = runtime.getSchedulerClient();

// List all schedules for an agent
List<WorkflowSchedule> all = schedules.getAllSchedules("daily_report");

// Pause and resume (by wire name). A pause reason is retained by Conductor.
schedules.pauseSchedule("daily_report-daily", "quarter-end freeze");
schedules.resumeSchedule("daily_report-daily");

// Delete (by wire name)
schedules.deleteSchedule("daily_report-daily");

// Preview the next N fire times for a cron expression
List<Long> next = schedules.getNextFewSchedules("0 9 * * *", null, null, 5); // epoch millis
```

## Native schedule fields

`SaveScheduleRequest` accepts the schedule name, cron expression, timezone, pause/catchup flags, optional start/end timestamps, description, and a `StartWorkflowRequest`. The `StartWorkflowRequest` identifies the deployed agent workflow and carries its fixed input.

## Migration from the AI scheduling facade

`AgentRuntime.schedules()`, `Schedule`, and `ScheduleInfo` are not part of the AI SDK. Obtain the shared client with `runtime.getSchedulerClient()`, build a `SaveScheduleRequest` directly, and use `WorkflowSchedule` for retrieved schedules. Schedule creation, update, and deletion are explicit; deploying an agent does not reconcile schedules.

## Cron syntax

Standard 5-field: `minute hour day-of-month month day-of-week`

```
0 9 * * *         every day at 9:00 AM
0 */6 * * *       every 6 hours
0 8 * * MON-FRI   weekdays at 8 AM
30 17 1 * *       1st of every month at 5:30 PM
```
