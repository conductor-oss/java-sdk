package timetracking;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import timetracking.workers.*;
import java.util.List;
import java.util.Map;

public class TimeTrackingExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 606: Time Tracking ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("ttk_submit", "ttk_validate", "ttk_approve", "ttk_process"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new SubmitWorker(), new ValidateWorker(), new ApproveWorker(), new ProcessWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String workflowId = client.startWorkflow("ttk_time_tracking", 1,
                Map.of("employeeId", "EMP-500", "weekEnding", "2024-03-22", "entries", 5));
        System.out.println("  Workflow ID: " + workflowId);
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Timesheet ID: " + workflow.getOutput().get("timesheetId"));
        System.out.println("  Total hours: " + workflow.getOutput().get("totalHours"));
        System.out.println("  Processed: " + workflow.getOutput().get("processed"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
