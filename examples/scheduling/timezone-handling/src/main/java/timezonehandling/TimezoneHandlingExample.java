package timezonehandling;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import timezonehandling.workers.*;
import java.util.List;
import java.util.Map;

public class TimezoneHandlingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 407: Timezone Handling ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("tz_detect_zone", "tz_convert_time", "tz_schedule_job", "tz_execute_job"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new DetectZoneWorker(), new ConvertTimeWorker(), new ScheduleJobWorker(), new ExecuteJobWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("timezone_handling_407", 1, Map.of("userId", "user-jp-442", "requestedTime", "2026-03-09T02:00:00+09:00", "jobName", "daily-backup"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
